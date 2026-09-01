package com.teamnative.moil.domain.oauth.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.teamnative.moil.global.config.OauthProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class AppleIdTokenVerifier(
    private val objectMapper: ObjectMapper,
    private val oauthProperties: OauthProperties,
) {
    private val restClient = RestClient.create()

    fun verify(idToken: String): AppleIdToken {
        val header = parseHeader(idToken)
        val publicKey = findPublicKey(header.kid)
        val claims = try {
            Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .payload
        } catch (exception: JwtException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token signature is invalid.")
        } catch (exception: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token is invalid.")
        }

        val issuer = claims["iss", String::class.java]
        if (issuer != APPLE_ISSUER) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token issuer is invalid.")
        }

        if (!hasExpectedAudience(claims["aud"])) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token audience is invalid.")
        }

        return AppleIdToken(
            subject = claims.subject,
            email = claims["email", String::class.java],
        )
    }

    private fun parseHeader(idToken: String): AppleTokenHeader {
        val header = idToken.split(".").getOrNull(0)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token is invalid.")
        val json = String(Base64.getUrlDecoder().decode(header), StandardCharsets.UTF_8)

        return objectMapper.readValue(json)
    }

    private fun findPublicKey(kid: String): PublicKey {
        val jwks = restClient
            .get()
            .uri(APPLE_KEYS_URL)
            .retrieve()
            .body(AppleJwks::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple public keys response is empty.")
        val key = jwks.keys.firstOrNull { it.kid == kid }
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token key is unknown.")

        return key.toPublicKey()
    }

    private fun hasExpectedAudience(audience: Any?): Boolean {
        val clientId = requireSetting(oauthProperties.apple.clientId, "APPLE_CLIENT_ID")

        return when (audience) {
            is String -> audience == clientId
            is Collection<*> -> audience.contains(clientId)
            else -> false
        }
    }

    private fun AppleJwk.toPublicKey(): PublicKey {
        if (kty != "RSA") {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apple id_token key type is unsupported.")
        }

        val modulus = BigInteger(1, Base64.getUrlDecoder().decode(n))
        val exponent = BigInteger(1, Base64.getUrlDecoder().decode(e))
        val keySpec = RSAPublicKeySpec(modulus, exponent)

        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun requireSetting(value: String, name: String): String =
        value.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$name is required.")

    data class AppleIdToken(
        val subject: String,
        val email: String?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AppleTokenHeader(
        val kid: String,
    )

    data class AppleJwks(
        val keys: List<AppleJwk>,
    )

    data class AppleJwk(
        val kty: String,
        val kid: String,
        val n: String,
        val e: String,
        @JsonProperty("alg")
        val algorithm: String?,
    )

    companion object {
        private const val APPLE_ISSUER = "https://appleid.apple.com"
        private const val APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys"
    }
}
