package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.VerifiedSignupSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface VerifiedSignupSessionRepository : JpaRepository<VerifiedSignupSession, String> {
    fun deleteByExpiresAtBefore(now: Instant)
}
