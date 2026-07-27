package com.teamnative.moil.domain.auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "login_sessions")
data class LoginSession(
    @Id
    val accessToken: String,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val refreshToken: String,

    @Column(nullable = false)
    val expiresAt: Instant,
)
