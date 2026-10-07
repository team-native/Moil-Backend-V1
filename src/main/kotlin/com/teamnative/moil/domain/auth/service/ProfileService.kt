package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.ProfileResponse
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val userAccountRepository: UserAccountRepository,
) {

    @Transactional(readOnly = true)
    fun get(user: UserAccount): ProfileResponse = ProfileResponse(
        userId = user.id,
        name = user.name,
        email = user.email,
    )

    @Transactional
    fun update(user: UserAccount, name: String): ProfileResponse {
        val updatedUser = userAccountRepository.save(user.copy(name = name.trim()))

        return ProfileResponse(
            userId = updatedUser.id,
            name = updatedUser.name,
            email = updatedUser.email,
        )
    }
}
