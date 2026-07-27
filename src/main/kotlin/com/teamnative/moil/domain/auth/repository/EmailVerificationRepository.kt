package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.EmailVerification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EmailVerificationRepository : JpaRepository<EmailVerification, String> {
    fun deleteByExpiresAtBefore(now: Instant)
}
