package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.RefreshTokenResponse
import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.global.config.JwtProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.lang.Exception
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class TokenRefreshService(
    private val loginSessionRepository: LoginSessionRepository,
    private val userAccountRepository: UserAccountRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {

    @Transactional
    fun refresh(authorization: String?, refreshToken: String): RefreshTokenResponse {
        val accessToken = authorization.extractBearerToken()
        val claims = accessToken.validateJwtAllowExpired()
        val session = loginSessionRepository.findByAccessToken(accessToken)
            ?: throw unauthorized()

        if (session.refreshToken != refreshToken) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.")
        }

        val now = Instant.now(clock)
        if (!session.expiresAt.isBefore(now)) {
            return RefreshTokenResponse(
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                tokenType = "Bearer",
                expiresIn = jwtProperties.accessTokenExpiresIn,
            )
        }

        val user = userAccountRepository.findById(claims.userId).orElse(null)
            ?: throw unauthorized()
        val newAccessToken = jwtProvider.generateAccessToken(user)
        val newRefreshToken = "refresh_${UUID.randomUUID()}"

        loginSessionRepository.delete(session)
        loginSessionRepository.save(
            LoginSession(
                sessionId = UUID.randomUUID().toString(),
                accessToken = newAccessToken,
                userId = session.userId,
                refreshToken = newRefreshToken,
                expiresAt = now.plusSeconds(jwtProperties.accessTokenExpiresIn),
            ),
        )

        return RefreshTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.accessTokenExpiresIn,
        )
    }

    private fun String?.extractBearerToken(): String =
        this
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw unauthorized()

    private fun String.validateJwtAllowExpired(): JwtProvider.JwtClaims =
        try {
            jwtProvider.validateAllowExpired(this)
        } catch (exception: Exception) {
            throw unauthorized()
        }

    private fun unauthorized(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인되어 있지 않습니다.")

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
