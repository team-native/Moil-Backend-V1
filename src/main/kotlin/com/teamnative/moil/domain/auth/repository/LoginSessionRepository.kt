package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.LoginSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LoginSessionRepository : JpaRepository<LoginSession, String> {
    fun deleteByUserId(userId: Long)
}
