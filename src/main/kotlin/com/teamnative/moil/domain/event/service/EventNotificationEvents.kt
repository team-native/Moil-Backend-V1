package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.event.model.EventAttendanceStatus

data class EventCreatedNotification(
    val actorUserId: Long,
    val groupId: Long,
    val eventId: Long,
    val title: String,
)

data class EventUpdatedNotification(
    val actorUserId: Long,
    val groupId: Long,
    val eventId: Long,
    val title: String,
)

data class AttendanceUpdatedNotification(
    val actorUserId: Long,
    val groupId: Long,
    val eventId: Long,
    val creatorUserId: Long,
    val title: String,
    val status: EventAttendanceStatus,
)
