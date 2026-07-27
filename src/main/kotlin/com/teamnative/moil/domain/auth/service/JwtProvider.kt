package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.global.config.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JwtProvider(
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {
    private val signingKey = Keys.hmacShaKeyFor(
        jwtProperties.secret.toByteArray(StandardCharsets.UTF_8),
    )

    fun generateAccessToken(user: UserAccount, issuedAt: Instant = Instant.now(clock)): String {
        val expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenExpiresIn)

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("name", user.name)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()
    }

    fun validate(accessToken: String): JwtClaims =
        parseClaims(accessToken).let { claims ->
            JwtClaims(
                userId = claims.subject.toLong(),
                email = claims["email", String::class.java],
                name = claims["name", String::class.java],
            )
        }

    fun validateAllowExpired(accessToken: String): JwtClaims =
        parseClaimsAllowExpired(accessToken).let { claims ->
            JwtClaims(
                userId = claims.subject.toLong(),
                email = claims["email", String::class.java],
                name = claims["name", String::class.java],
            )
        }

    private fun parseClaims(accessToken: String): Claims =
        Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(accessToken)
            .payload

    private fun parseClaimsAllowExpired(accessToken: String): Claims =
        try {
            parseClaims(accessToken)
        } catch (exception: ExpiredJwtException) {
            exception.claims
        }

    data class JwtClaims(
        val userId: Long,
        val email: String,
        val name: String,
    )
}
