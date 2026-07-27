package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendEmailCodeRequest(
    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,
)
