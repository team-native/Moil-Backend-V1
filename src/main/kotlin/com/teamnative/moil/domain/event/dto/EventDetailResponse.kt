package com.teamnative.moil.domain.event.dto

import java.time.Instant

data class EventDetailResponse(
    val eventId: Long,
    val groupId: Long,
    val title: String,
    val memo: String?,
    val startsAt: Instant,
    val endsAt: Instant,
    val creatorId: Long,
    val updaterId: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
)
