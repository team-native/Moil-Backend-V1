package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.global.config.JwtProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class AuthTokenService(
    private val loginSessionRepository: LoginSessionRepository,
    private val jwtProvider: JwtProvider,
    private val jwtProperties: JwtProperties,
    private val clock: Clock,
) {

    @Transactional
    fun issue(user: UserAccount): IssuedToken =
        issue(user, removeExistingSessions = false)

    @Transactional
    fun issueForLogin(user: UserAccount): IssuedToken =
        issue(user, removeExistingSessions = true)

    private fun issue(user: UserAccount, removeExistingSessions: Boolean): IssuedToken {
        if (removeExistingSessions) {
            loginSessionRepository.deleteByUserId(user.id)
        }

        val accessToken = jwtProvider.generateAccessToken(user)
        val refreshToken = "refresh_${UUID.randomUUID()}"

        loginSessionRepository.save(
            LoginSession(
                sessionId = UUID.randomUUID().toString(),
                accessToken = accessToken,
                userId = user.id,
                refreshToken = refreshToken,
                expiresAt = Instant.now(clock).plusSeconds(jwtProperties.accessTokenExpiresIn),
            ),
        )

        return IssuedToken(
            userId = user.id,
            name = user.name,
            email = user.email,
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = jwtProperties.accessTokenExpiresIn,
        )
    }

    data class IssuedToken(
        val userId: Long,
        val name: String,
        val email: String,
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String,
        val expiresIn: Long,
    )

}
