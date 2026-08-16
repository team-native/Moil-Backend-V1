package com.teamnative.moil.domain.event.dto

data class EventDetailResponse(
    val eventId: Long,
    val groupId: Long,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: String?,
    val memo: String?,
    val members: List<EventMemberResponse>,
)

data class CreateEventResponse(
    val eventId: Long,
)
