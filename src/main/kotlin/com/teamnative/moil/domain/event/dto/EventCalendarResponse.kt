package com.teamnative.moil.domain.event.dto

import java.time.Instant

data class EventCalendarResponse(
    val eventId: Long,
    val title: String,
    val startsAt: Instant,
    val endsAt: Instant,
)
