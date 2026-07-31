package com.teamnative.moil.domain.event.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "events")
data class Event(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val groupId: Long,

    @Column(nullable = false)
    val creatorId: Long,

    @Column(nullable = false)
    val updaterId: Long,

    @Column(nullable = false, length = 100)
    val title: String,

    @Column(nullable = true, length = 1000)
    val memo: String? = null,

    @Column(nullable = true, length = 255)
    val location: String? = null,

    @Column(nullable = false)
    val startsAt: Instant,

    @Column(nullable = false)
    val endsAt: Instant,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,
)
