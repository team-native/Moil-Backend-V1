package com.teamnative.moil.domain.group.dto

import jakarta.validation.constraints.NotBlank

data class CreateGroupRequest(
    @field:NotBlank(message = "그룹 이름을 입력해주세요.")
    val name: String,
)
