package com.teamnative.moil.domain.oauth.service

import com.teamnative.moil.global.config.OauthProperties
import io.jsonwebtoken.Jwts
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.Date

@Component
class AppleClientSecretGenerator(
    private val clock: Clock,
    private val oauthProperties: OauthProperties,
) {

    fun generate(): String {
        oauthProperties.apple.clientSecret.takeIf { it.isNotBlank() }?.let {
            return it
        }

        val teamId = requireSetting(oauthProperties.apple.teamId, "APPLE_TEAM_ID")
        val keyId = requireSetting(oauthProperties.apple.keyId, "APPLE_KEY_ID")
        val clientId = requireSetting(oauthProperties.apple.clientId, "APPLE_CLIENT_ID")
        val privateKey = readPrivateKey(requireSetting(oauthProperties.apple.privateKey, "APPLE_PRIVATE_KEY"))
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plusSeconds(APPLE_CLIENT_SECRET_EXPIRES_IN)

        return Jwts.builder()
            .header()
            .keyId(keyId)
            .and()
            .issuer(teamId)
            .subject(clientId)
            .audience()
            .add("https://appleid.apple.com")
            .and()
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(privateKey, Jwts.SIG.ES256)
            .compact()
    }

    private fun readPrivateKey(value: String): PrivateKey {
        val normalized = value
            .replace("\\n", "\n")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val decoded = Base64.getDecoder().decode(normalized.toByteArray(StandardCharsets.UTF_8))
        val keySpec = PKCS8EncodedKeySpec(decoded)

        return KeyFactory.getInstance("EC").generatePrivate(keySpec)
    }

    private fun requireSetting(value: String, name: String): String =
        value.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$name is required.")

    companion object {
        private const val APPLE_CLIENT_SECRET_EXPIRES_IN = 15_552_000L
    }
}
