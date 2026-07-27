package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.NotBlank

data class ChangePasswordRequest(
    @field:NotBlank(message = "기존 비밀번호를 입력해주세요.")
    val origin: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요.")
    val newpwd: String,

    @field:NotBlank(message = "새 비밀번호 확인값을 입력해주세요.")
    val checkpwd: String,
)
