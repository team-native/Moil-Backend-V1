package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class ResetPasswordRequest(
    @field:NotBlank(message = "비밀번호 초기화 세션을 입력해주세요.")
    val sessionId: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String,

    @field:NotBlank(message = "비밀번호 확인값을 입력해주세요.")
    val pwd: String,
)
