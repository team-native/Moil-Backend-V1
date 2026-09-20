package com.teamnative.moil.domain.availability.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class SaveAvailabilityRequest(
    @field:NotBlank(message = "가능 날짜를 입력해주세요.")
    @field:Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "가능 날짜는 yyyy-MM-dd 형식으로 입력해주세요.",
    )
    val date: String,

    @field:NotEmpty(message = "가능한 시간대를 1개 이상 입력해주세요.")
    @field:Valid
    val timeSlots: List<AvailabilitySlotRequest>,
)

data class AvailabilitySlotRequest(
    @field:NotBlank(message = "시작 시간을 입력해주세요.")
    @field:Pattern(
        regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d",
        message = "시작 시간은 HH:mm 형식으로 입력해주세요.",
    )
    val startTime: String,

    @field:NotBlank(message = "종료 시간을 입력해주세요.")
    @field:Pattern(
        regexp = "(?:[01]\\d|2[0-3]):[0-5]\\d",
        message = "종료 시간은 HH:mm 형식으로 입력해주세요.",
    )
    val endTime: String,
)
