package com.teamnative.moil.domain.availability.dto

data class AvailabilitySummaryResponse(
    val eventId: Long,
    val date: String,
    val participantCount: Int,
    val respondedCount: Int,
    val timeSlots: List<AvailabilitySummarySlotResponse>,
)

data class AvailabilitySummarySlotResponse(
    val startTime: String,
    val endTime: String,
    val availableCount: Int,
    val availableMemberIds: List<Long>,
    val isAvailableForEveryone: Boolean,
)
