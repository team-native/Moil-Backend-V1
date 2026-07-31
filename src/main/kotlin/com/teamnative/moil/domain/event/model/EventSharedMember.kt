package com.teamnative.moil.domain.event.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "event_shared_members",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_shared_members_event_user", columnNames = ["event_id", "user_id"]),
    ],
)
data class EventSharedMember(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val eventId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val createdAt: Instant,
)
