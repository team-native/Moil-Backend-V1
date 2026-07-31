package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.auth.dto.EmailVerificationStep
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class SignupService(
    private val emailVerificationService: EmailVerificationService,
    private val userAccountRepository: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
) {

    @Transactional
    fun confirm(sessionId: String, password: String, pwd: String): AuthTokenResponse {
        if (password != pwd) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.")
        }

        val verifiedSession = emailVerificationService.consumeVerifiedSession(sessionId, EmailVerificationStep.SIGNUP)
        val email = verifiedSession.email

        if (userAccountRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.")
        }

        val encodedPassword = passwordEncoder.encode(password)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "비밀번호 암호화에 실패했습니다.")

        val user = try {
            userAccountRepository.saveAndFlush(
                UserAccount(
                    name = verifiedSession.name ?: email.substringBefore("@"),
                    email = email,
                    passwordHash = encodedPassword,
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.")
        }
        val token = authTokenService.issue(user)

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
}
