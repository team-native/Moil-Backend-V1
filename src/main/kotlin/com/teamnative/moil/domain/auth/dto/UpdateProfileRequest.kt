package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:NotBlank(message = "이름을 입력해주세요.")
    @field:Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    val name: String,
)
