package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotNull

data class TransferGroupOwnerRequest(
    @field:NotNull(message = "대상 멤버를 입력해주세요.")
    val targetUserId: Long?,
)
