package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class PasswordResetService(
    private val emailVerificationService: EmailVerificationService,
    private val userAccountRepository: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun reset(sessionId: String, password: String, pwd: String) {
        if (password != pwd) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.")
        }

        val email = emailVerificationService.consumeVerifiedSession(
            sessionId = sessionId,
            notFoundMessage = "비밀번호 초기화 세션이 없거나 만료되었습니다.",
        )
        val user = userAccountRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "비밀번호 초기화 세션이 없거나 만료되었습니다.")
        val encodedPassword = passwordEncoder.encode(password)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "비밀번호 암호화에 실패했습니다.")

        userAccountRepository.save(user.copy(passwordHash = encodedPassword))
    }
}
