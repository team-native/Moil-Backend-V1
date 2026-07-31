package com.teamnative.moil.domain.event.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class EventDetailResponse(
    val eventId: Long,
    val groupId: Long,
    val title: String,
    val date: String,
    @get:JsonProperty("isAllDay")
    val isAllDay: Boolean,
    val startTime: String?,
    val endTime: String?,
    val location: String?,
    val members: List<EventMemberResponse>,
)

data class CreateEventResponse(
    val eventId: Long,
)
