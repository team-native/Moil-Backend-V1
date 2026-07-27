package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserAccountRepository : JpaRepository<UserAccount, Long> {
    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): UserAccount?
}
