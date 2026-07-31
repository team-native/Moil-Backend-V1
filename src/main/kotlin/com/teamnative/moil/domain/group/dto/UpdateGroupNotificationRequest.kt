package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotNull

data class UpdateGroupNotificationRequest(
    @field:NotNull(message = "알림 설정 여부를 입력해주세요.")
    val enabled: Boolean?,
)
