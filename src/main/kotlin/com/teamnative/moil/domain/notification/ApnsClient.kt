package com.teamnative.moil.domain.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.teamnative.moil.global.config.ApnsProperties
import io.jsonwebtoken.Jwts
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.Date

data class ApnsPayload(
    val type: String,
    val groupId: Long,
    val eventId: Long? = null,
    val title: String,
    val body: String,
)

data class ApnsDeliveryResult(
    val successful: Boolean,
    val invalidToken: Boolean = false,
)

@Component
class ApnsClient(
    private val properties: ApnsProperties,
    private val objectMapper: ObjectMapper,
) {
    private val httpClient = HttpClient.newHttpClient()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val privateKey: PrivateKey? by lazy { properties.privateKey.takeIf { it.isNotBlank() }?.toPrivateKey() }
    @Volatile
    private var cachedToken: CachedJwt? = null

    fun send(deviceToken: String, payload: ApnsPayload): ApnsDeliveryResult {
        if (!properties.enabled) return ApnsDeliveryResult(successful = true)

        val key = privateKey ?: error("APNS_PRIVATE_KEY is required when APNS_ENABLED is true.")
        val jwt = accessToken(key)
        val body = objectMapper.writeValueAsString(
            mapOf(
                "aps" to mapOf(
                    "alert" to mapOf("title" to payload.title, "body" to payload.body),
                    "sound" to "default",
                ),
                "type" to payload.type,
                "groupId" to payload.groupId,
                "eventId" to payload.eventId,
            ).filterValues { it != null },
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${endpoint()}/3/device/$deviceToken"))
            .header("authorization", "bearer $jwt")
            .header("apns-topic", properties.bundleId)
            .header("apns-push-type", "alert")
            .header("apns-priority", "10")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        return try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            when {
                response.statusCode() in 200..299 -> ApnsDeliveryResult(successful = true)
                response.statusCode() == 400 || response.statusCode() == 410 -> {
                    val reason = runCatching { objectMapper.readTree(response.body()).path("reason").asText() }.getOrDefault("")
                    ApnsDeliveryResult(successful = false, invalidToken = reason in INVALID_TOKEN_REASONS)
                }
                else -> ApnsDeliveryResult(successful = false)
            }
        } catch (exception: Exception) {
            logger.warn("APNs delivery failed for a device token.", exception)
            ApnsDeliveryResult(successful = false)
        }
    }

    private fun accessToken(key: PrivateKey): String {
        val now = Instant.now()
        cachedToken?.takeIf { it.expiresAt.isAfter(now.plusSeconds(60)) }?.let { return it.value }
        val value = Jwts.builder()
            .header().keyId(properties.keyId).and()
            .issuer(properties.teamId)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(3600)))
            .signWith(key, Jwts.SIG.ES256)
            .compact()
        cachedToken = CachedJwt(value, now.plusSeconds(3600))
        return value
    }

    private fun endpoint(): String = when (properties.environment.lowercase()) {
        "production", "prod" -> "https://api.push.apple.com"
        else -> "https://api.sandbox.push.apple.com"
    }

    private fun String.toPrivateKey(): PrivateKey {
        val encoded = replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s"), "")
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded)))
    }

    private data class CachedJwt(val value: String, val expiresAt: Instant)

    companion object {
        private val INVALID_TOKEN_REASONS = setOf("BadDeviceToken", "DeviceTokenNotForTopic", "Unregistered")
    }
}
