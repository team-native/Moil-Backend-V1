package com.teamnative.moil.domain.oauth.model

import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_social_accounts_provider_provider_user", columnNames = ["provider", "provider_user_id"]),
        UniqueConstraint(name = "uk_social_accounts_provider_user", columnNames = ["provider", "user_id"]),
    ],
)
data class SocialAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: SocialLoginType,

    @Column(name = "provider_user_id", nullable = false, length = 255)
    val providerUserId: String,

    @Column(nullable = false, length = 255)
    val email: String,

    @Column(nullable = false)
    val createdAt: Instant,
)
