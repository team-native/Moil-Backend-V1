package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotBlank

data class JoinGroupRequest(
    @field:NotBlank(message = "초대 코드를 입력해주세요.")
    val inviteCode: String,
    val nickname: String? = null,
    val colorId: String? = null,
)
