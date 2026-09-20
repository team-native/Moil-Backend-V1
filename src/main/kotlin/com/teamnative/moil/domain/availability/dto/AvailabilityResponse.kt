package com.teamnative.moil.domain.availability.dto

data class AvailabilityResponse(
    val eventId: Long,
    val date: String,
    val members: List<AvailabilityMemberResponse>,
)

data class AvailabilityMemberResponse(
    val userId: Long,
    val nickname: String,
    val colorId: String?,
    val timeSlots: List<AvailabilityTimeSlotResponse>,
)

data class MyAvailabilityResponse(
    val eventId: Long,
    val date: String,
    val userId: Long,
    val timeSlots: List<AvailabilityTimeSlotResponse>,
)

data class AvailabilityTimeSlotResponse(
    val startTime: String,
    val endTime: String,
)
