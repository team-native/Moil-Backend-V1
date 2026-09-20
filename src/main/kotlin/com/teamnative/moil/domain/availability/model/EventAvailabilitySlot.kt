package com.teamnative.moil.domain.availability.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "event_availability_slots")
data class EventAvailabilitySlot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val availabilityId: Long,

    @Column(nullable = false)
    val startsAt: Instant,

    @Column(nullable = false)
    val endsAt: Instant,
)
