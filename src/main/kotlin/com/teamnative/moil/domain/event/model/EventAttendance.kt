package com.teamnative.moil.domain.event.model

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
    name = "event_attendances",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_event_attendances_event_user", columnNames = ["event_id", "user_id"]),
    ],
)
data class EventAttendance(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val eventId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: EventAttendanceStatus,

    @Column(nullable = false)
    val updatedAt: Instant,
)
