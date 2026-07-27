package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class LoginService(
    private val userAccountRepository: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
) {

    fun login(email: String, password: String): AuthTokenResponse {
        val user = userAccountRepository.findByEmail(email)
            ?: throw invalidCredentials()

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw invalidCredentials()
        }

        val token = authTokenService.issueForLogin(user)

        return AuthTokenResponse(
            userId = token.userId,
            name = token.name,
            email = token.email,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenType = token.tokenType,
            expiresIn = token.expiresIn,
        )
    }

    private fun invalidCredentials(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.")
}
