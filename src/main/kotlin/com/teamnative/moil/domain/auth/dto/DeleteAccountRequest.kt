package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class DeleteAccountRequest(
    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요.")
    val password: String,

    @field:NotNull(message = "데이터 잔류 여부를 입력해주세요.")
    val leftData: Boolean?,
)
