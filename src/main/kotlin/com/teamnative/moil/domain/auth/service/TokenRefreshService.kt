package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.RefreshTokenResponse
import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class TokenRefreshService(
    private val loginSessionRepository: LoginSessionRepository,
    private val clock: Clock,
) {

    @Transactional
    fun refresh(authorization: String?, refreshToken: String): RefreshTokenResponse {
        val accessToken = authorization.extractBearerToken()
        val session = loginSessionRepository.findById(accessToken).orElse(null)
            ?: throw unauthorized()

        if (session.expiresAt.isBefore(Instant.now(clock))) {
            loginSessionRepository.delete(session)
            throw unauthorized()
        }

        if (session.refreshToken != refreshToken) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.")
        }

        val newAccessToken = "access_${UUID.randomUUID()}"
        val newRefreshToken = "refresh_${UUID.randomUUID()}"

        loginSessionRepository.delete(session)
        loginSessionRepository.save(
            LoginSession(
                accessToken = newAccessToken,
                userId = session.userId,
                refreshToken = newRefreshToken,
                expiresAt = Instant.now(clock).plusSeconds(ACCESS_TOKEN_EXPIRES_IN_SECONDS),
            ),
        )

        return RefreshTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = ACCESS_TOKEN_EXPIRES_IN_SECONDS,
        )
    }

    private fun String?.extractBearerToken(): String =
        this
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw unauthorized()

    private fun unauthorized(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인되어 있지 않습니다.")

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val ACCESS_TOKEN_EXPIRES_IN_SECONDS = 3600L
    }
}
