package com.teamnative.moil.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "verified_signup_sessions")
data class VerifiedSignupSession(
    @Id
    val sessionId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(nullable = false)
    val expiresAt: Instant,
)
