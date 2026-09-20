package com.teamnative.moil.domain.availability.dto

import com.fasterxml.jackson.annotation.JsonProperty

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
    @get:JsonProperty("isAvailableForEveryone")
    val isAvailableForEveryone: Boolean,
)
