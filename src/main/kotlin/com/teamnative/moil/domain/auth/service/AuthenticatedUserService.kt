package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@Service
class AuthenticatedUserService(
    private val loginSessionRepository: LoginSessionRepository,
    private val userAccountRepository: UserAccountRepository,
    private val clock: Clock,
) {

    @Transactional
    fun getByAuthorizationHeader(authorization: String?): UserAccount {
        val token = authorization
            .extractBearerToken()

        val session = loginSessionRepository.findById(token).orElse(null)
            ?: throw unauthorized()

        if (session.expiresAt.isBefore(Instant.now(clock))) {
            loginSessionRepository.delete(session)
            throw unauthorized()
        }

        return userAccountRepository.findById(session.userId).orElse(null)
            ?: throw unauthorized()
    }

    @Transactional
    fun logout(authorization: String?) {
        val token = authorization.extractBearerToken()
        val session = loginSessionRepository.findById(token).orElse(null)
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

    private fun unauthorized(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인되어 있지 않습니다.")

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
