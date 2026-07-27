package com.teamnative.moil.domain.auth.controller

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.auth.dto.ConfirmSignupRequest
import com.teamnative.moil.domain.auth.dto.SendEmailCodeRequest
import com.teamnative.moil.domain.auth.dto.SendEmailCodeResponse
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeRequest
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeResponse
import com.teamnative.moil.domain.auth.service.EmailVerificationService
import com.teamnative.moil.domain.auth.service.SignupService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val emailVerificationService: EmailVerificationService,
    private val signupService: SignupService,
) {

    @GetMapping
    fun auth(): ApiResponse<Nothing> = ApiResponse.empty("/auth")

    @PostMapping("/send-code")
    fun sendCode(
        @Valid @RequestBody request: SendEmailCodeRequest,
    ): ApiResponse<SendEmailCodeResponse> =
        ApiResponse.success(
            message = "인증 코드가 발송되었습니다.",
            data = emailVerificationService.sendCode(request.email),
        )

    @PostMapping("/verify-code")
    fun verifyCode(
        @Valid @RequestBody request: VerifyEmailCodeRequest,
    ): ApiResponse<VerifyEmailCodeResponse> =
        ApiResponse.success(
            message = "인증이 완료되었습니다.",
            data = emailVerificationService.verifyCode(request.verifyId, request.code),
        )

    @PostMapping("/confirm")
    fun confirm(
        @Valid @RequestBody request: ConfirmSignupRequest,
    ): ApiResponse<AuthTokenResponse> =
        ApiResponse.success(
            message = "회원가입이 완료되었습니다.",
            data = signupService.confirm(request.sessionId, request.password, request.pwd),
        )
}
