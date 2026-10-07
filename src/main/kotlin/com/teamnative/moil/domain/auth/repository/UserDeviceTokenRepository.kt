package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.UserDeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserDeviceTokenRepository : JpaRepository<UserDeviceToken, Long> {
    fun findByToken(token: String): UserDeviceToken?

    fun deleteByUserIdAndToken(userId: Long, token: String)

    fun deleteByUserId(userId: Long)
}
