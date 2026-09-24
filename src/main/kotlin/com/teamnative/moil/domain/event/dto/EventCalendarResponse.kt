package com.teamnative.moil.domain.event.dto

data class EventCalendarResponse(
    val eventId: Long,
    val title: String,
    val startDate: String,
    val endDate: String,
    val location: String?,
    val memo: String?,
    val members: List<EventMemberResponse>,
    val myAttendanceStatus: com.teamnative.moil.domain.event.model.EventAttendanceStatus?,
    val attendingCount: Int,
)

data class EventMemberResponse(
    val userId: Long,
    val nickname: String,
    val colorId: String?,
    val imagePath: String?,
)
