package com.teamnative.moil.domain.event.dto

data class EventCalendarResponse(
    val eventId: Long,
    val title: String,
    val date: String,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val memo: String?,
    val members: List<EventMemberResponse>,
)

data class EventMemberResponse(
    val userId: Long,
    val nickname: String,
    val colorId: String?,
    val imagePath: String?,
)
