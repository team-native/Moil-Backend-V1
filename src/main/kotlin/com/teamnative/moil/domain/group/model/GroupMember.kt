package com.teamnative.moil.domain.group.model

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
    name = "group_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_group_members_group_user", columnNames = ["groupId", "userId"]),
        UniqueConstraint(name = "uk_group_members_owner_group", columnNames = ["ownerGroupId"]),
    ],
)
data class GroupMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val groupId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = true)
    val ownerGroupId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: GroupRole,

    @Column(nullable = false)
    val notificationEnabled: Boolean = true,

    @Column(nullable = false)
    val joinedAt: Instant,
)
