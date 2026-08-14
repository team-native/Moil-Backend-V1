package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateGroupMemberProfileRequest(
    @field:NotBlank(message = "닉네임을 입력해주세요.")
    @field:Size(max = 10, message = "닉네임은 1자 이상 10자 이하로 입력해야 합니다.")
    val nickname: String,

    val colorId: String? = null,
    val imagePath: String? = null,
)
