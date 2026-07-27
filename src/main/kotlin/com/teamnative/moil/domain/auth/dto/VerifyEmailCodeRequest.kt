package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class VerifyEmailCodeRequest(
    @field:NotBlank(message = "인증 요청 ID를 입력해주세요.")
    val verifyId: String,

    @field:NotBlank(message = "인증 코드를 입력해주세요.")
    val code: String,
)
