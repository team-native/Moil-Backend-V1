package com.teamnative.moil.domain.event.dto

import jakarta.validation.constraints.NotBlank

data class UpdateEventRequest(
    @field:NotBlank(message = "일정 제목을 입력해주세요.")
    val title: String,

    val memo: String? = null,

    @field:NotBlank(message = "시작 시간을 입력해주세요.")
    val startsAt: String,

    @field:NotBlank(message = "종료 시간을 입력해주세요.")
    val endsAt: String,
)
