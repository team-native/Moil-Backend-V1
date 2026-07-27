package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.lang.Exception
import java.time.Clock
import java.time.Instant

@Service
class AuthenticatedUserService(
    private val loginSessionRepository: LoginSessionRepository,
    private val userAccountRepository: UserAccountRepository,
    private val jwtProvider: JwtProvider,
    private val clock: Clock,
) {

    @Transactional
    fun getByAuthorizationHeader(authorization: String?): UserAccount {
        val token = authorization
            .extractBearerToken()

        val claims = token.validateJwt()
        val session = loginSessionRepository.findByAccessToken(token)
            ?: throw unauthorized()

        if (session.expiresAt.isBefore(Instant.now(clock))) {
            loginSessionRepository.delete(session)
            throw unauthorized()
        }

        return userAccountRepository.findById(claims.userId).orElse(null)
            ?: throw unauthorized()
    }

    @Transactional
    fun logout(authorization: String?) {
        val token = authorization.extractBearerToken()
        val session = loginSessionRepository.findByAccessToken(token)
            ?: throw unauthorized()

        loginSessionRepository.delete(session)
    }

    private fun String?.extractBearerToken(): String =
        this
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw unauthorized()

    private fun String.validateJwt(): JwtProvider.JwtClaims =
        try {
            jwtProvider.validate(this)
        } catch (exception: Exception) {
            throw unauthorized()
        }

    private fun unauthorized(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인되어 있지 않습니다.")

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
