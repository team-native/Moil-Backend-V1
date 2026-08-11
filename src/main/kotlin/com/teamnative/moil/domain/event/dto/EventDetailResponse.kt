package com.teamnative.moil.domain.event.dto

data class EventDetailResponse(
    val eventId: Long,
    val groupId: Long,
    val title: String,
    val date: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val memo: String?,
    val members: List<EventMemberResponse>,
)

data class CreateEventResponse(
    val eventId: Long,
)
