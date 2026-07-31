package com.teamnative.moil.domain.auth.model

import com.teamnative.moil.domain.auth.dto.EmailVerificationStep
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "email_verifications")
data class EmailVerification(
    @Id
    val verifyId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(length = 100)
    val name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val step: EmailVerificationStep? = null,

    @Column(nullable = false, length = 6)
    val code: String,

    @Column(nullable = false)
    val expiresAt: Instant,
)
