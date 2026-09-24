package com.teamnative.moil.domain.event.dto

import com.teamnative.moil.domain.event.model.EventAttendanceStatus
import jakarta.validation.constraints.NotBlank

data class UpdateEventAttendanceRequest(
    @field:NotBlank(message = "참석 상태를 입력해주세요.")
    val status: String?,
)

data class EventAttendanceResponse(
    val status: EventAttendanceStatus,
    val updatedAt: java.time.Instant,
)

data class EventAttendanceSummaryResponse(
    val myStatus: EventAttendanceStatus?,
    val participantCount: Int,
    val attendingCount: Int,
    val declinedCount: Int,
    val members: List<EventAttendanceMemberResponse>,
)

data class EventAttendanceMemberResponse(
    val memberId: Long,
    val nickname: String,
    val colorId: String?,
    val status: EventAttendanceStatus?,
)
