package com.teamnative.moil.domain.availability.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "event_availabilities",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_event_availabilities_event_user_date",
            columnNames = ["event_id", "user_id", "available_date"],
        ),
    ],
)
data class EventAvailability(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val eventId: Long,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val availableDate: LocalDate,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,
)
