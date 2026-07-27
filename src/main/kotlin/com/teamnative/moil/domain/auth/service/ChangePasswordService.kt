package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ChangePasswordService(
    private val userAccountRepository: UserAccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun change(user: UserAccount, origin: String, newpwd: String, checkpwd: String) {
        if (!passwordEncoder.matches(origin, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다.")
        }

        if (newpwd != checkpwd) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다.")
        }

        val encodedPassword = passwordEncoder.encode(newpwd)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "비밀번호 암호화에 실패했습니다.")

        userAccountRepository.save(user.copy(passwordHash = encodedPassword))
    }
}
