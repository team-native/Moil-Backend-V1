package com.teamnative.moil.domain.event.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateEventRequest(
    @field:NotBlank(message = "일정 제목을 입력해주세요.")
    @field:Size(max = 100, message = "일정 제목은 100자 이하로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "일정 시작 시간을 입력해주세요.")
    val startDate: String,

    @field:NotBlank(message = "일정 종료 시간을 입력해주세요.")
    val endDate: String,
    val location: String? = null,
    @field:Size(max = 1000, message = "일정 메모는 1000자 이하로 입력해주세요.")
    val memo: String? = null,
    val sharedMemberIds: List<Long> = emptyList(),
)
