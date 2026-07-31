package com.teamnative.moil.domain.group.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_group_members_group_user", columnNames = ["group_id", "user_id"]),
        UniqueConstraint(name = "uk_group_members_owner_group", columnNames = ["owner_group_id"]),
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
    var ownerGroupId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: GroupRole,

    @Column(nullable = false)
    val notificationEnabled: Boolean = true,

    @Column(nullable = false, length = 10)
    val nickname: String = "User",

    @Column(nullable = false, length = 20)
    val color: String = "RED",

    @Column(nullable = false)
    val joinedAt: Instant,
) {

    @PrePersist
    @PreUpdate
    fun syncOwnerGroupId() {
        ownerGroupId = if (role == GroupRole.OWNER) groupId else null
    }
}
