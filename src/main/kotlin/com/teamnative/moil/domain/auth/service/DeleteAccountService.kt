package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class DeleteAccountService(
    private val userAccountRepository: UserAccountRepository,
    private val loginSessionRepository: LoginSessionRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun delete(user: UserAccount, email: String, password: String, leftData: Boolean) {
        if (user.email != email || !passwordEncoder.matches(password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        applyCalendarDataPolicy(user, leftData)
        loginSessionRepository.deleteByUserId(user.id)
        userAccountRepository.delete(user)
    }

    private fun applyCalendarDataPolicy(user: UserAccount, leftData: Boolean) {
        // Events are not persisted yet. Keep this boundary so event data cleanup can be added here.
        if (leftData) {
            return
        }
    }
}
