package com.teamnative.moil.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class SendEmailCodeRequest(
    @field:Size(max = 100, message = "이름은 100자 이하여야 합니다.")
    val name: String? = null,

    @field:NotBlank(message = "이메일을 입력해주세요.")
    @field:Email(message = "이메일 형식이 올바르지 않습니다.")
    val email: String,

    @field:NotNull(message = "인증 단계를 입력해주세요.")
    val step: EmailVerificationStep?,
)

enum class EmailVerificationStep {
    SIGNUP,
    RESET,
}
