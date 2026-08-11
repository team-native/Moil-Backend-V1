package com.teamnative.moil.domain.event.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateEventRequest(
    @field:NotNull(message = "그룹을 입력해주세요.")
    val groupId: Long?,

    @field:NotBlank(message = "일정 제목을 입력해주세요.")
    @field:Size(max = 100, message = "일정 제목은 100자 이하로 입력해주세요.")
    val title: String,

    @field:NotBlank(message = "일정 날짜를 입력해주세요.")
    val date: String,

    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val memo: String? = null,
    val sharedMemberIds: List<Long> = emptyList(),
)
