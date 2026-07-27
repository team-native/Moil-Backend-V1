package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotBlank

data class CheckGroupInviteRequest(
    @field:NotBlank(message = "초대 코드를 입력해주세요.")
    val inviteCode: String,
)
