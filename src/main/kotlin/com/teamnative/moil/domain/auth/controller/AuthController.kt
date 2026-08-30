package com.teamnative.moil.domain.auth.controller

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.auth.dto.ChangePasswordRequest
import com.teamnative.moil.domain.auth.dto.ConfirmSignupRequest
import com.teamnative.moil.domain.auth.dto.DeleteAccountRequest
import com.teamnative.moil.domain.auth.dto.LoginRequest
import com.teamnative.moil.domain.auth.dto.ProfileResponse
import com.teamnative.moil.domain.auth.dto.RefreshTokenRequest
import com.teamnative.moil.domain.auth.dto.RefreshTokenResponse
import com.teamnative.moil.domain.auth.dto.ResetPasswordRequest
import com.teamnative.moil.domain.auth.dto.SendEmailCodeRequest
import com.teamnative.moil.domain.auth.dto.SendEmailCodeResponse
import com.teamnative.moil.domain.auth.dto.UpdateProfileRequest
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeRequest
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeResponse
import com.teamnative.moil.domain.auth.service.EmailVerificationService
import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.auth.service.ChangePasswordService
import com.teamnative.moil.domain.auth.service.DeleteAccountService
import com.teamnative.moil.domain.auth.service.LoginService
import com.teamnative.moil.domain.auth.service.PasswordResetService
import com.teamnative.moil.domain.auth.service.ProfileService
import com.teamnative.moil.domain.auth.service.SignupService
import com.teamnative.moil.domain.auth.service.TokenRefreshService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/auth")
class AuthController(
    private val emailVerificationService: EmailVerificationService,
    private val signupService: SignupService,
    private val loginService: LoginService,
    private val passwordResetService: PasswordResetService,
    private val authenticatedUserService: AuthenticatedUserService,
    private val changePasswordService: ChangePasswordService,
    private val tokenRefreshService: TokenRefreshService,
    private val deleteAccountService: DeleteAccountService,
    private val profileService: ProfileService,
) {

    @GetMapping
    fun auth(): ApiResponse<Nothing> = ApiResponse.empty("/auth")

    @PostMapping("/send-code")
    fun sendCode(
        @Valid @RequestBody request: SendEmailCodeRequest,
    ): ApiResponse<SendEmailCodeResponse> =
        ApiResponse.success(
            message = "인증 코드가 발송되었습니다.",
            data = emailVerificationService.sendCode(request.name, request.email, request.step!!),
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

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ApiResponse<AuthTokenResponse> =
        ApiResponse.success(
            message = "로그인되었습니다.",
            data = loginService.login(request.email, request.password),
        )

    @PostMapping("/reset-password")
    fun resetPassword(
        @Valid @RequestBody request: ResetPasswordRequest,
    ): ApiResponse<Nothing> {
        passwordResetService.reset(request.sessionId, request.password, request.pwd)

        return ApiResponse.empty("비밀번호가 변경되었습니다.")
    }

    @PostMapping("/change-password")
    fun changePassword(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        changePasswordService.change(user, request.origin, request.newpwd, request.checkpwd)

        return ApiResponse.empty("비밀번호가 변경되었습니다.")
    }

    @PatchMapping("/profile")
    fun updateProfile(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ApiResponse<ProfileResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "프로필이 변경되었습니다.",
            data = profileService.update(user, request.name),
        )
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization", required = false) authorization: String?,
    ): ApiResponse<Nothing> {
        authenticatedUserService.logout(authorization)

        return ApiResponse.empty("로그아웃되었습니다.")
    }

    @PostMapping("/refresh", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun refresh(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: RefreshTokenRequest,
    ): ApiResponse<RefreshTokenResponse> =
        refreshToken(authorization, request.refreshToken)

    @PostMapping("/refresh", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun refreshForm(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestParam("refresh_token", required = false) refreshToken: String?,
        @RequestParam("refreshToken", required = false) camelRefreshToken: String?,
    ): ApiResponse<RefreshTokenResponse> =
        refreshToken(authorization, refreshToken ?: camelRefreshToken)

    @PostMapping("/delete-account")
    fun deleteAccount(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: DeleteAccountRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        deleteAccountService.delete(user, request.email, request.password, request.leftData!!)

        return ApiResponse.empty("회원 탈퇴가 완료되었습니다.")
    }

    private fun refreshToken(
        authorization: String?,
        refreshToken: String?,
    ): ApiResponse<RefreshTokenResponse> {
        val token = refreshToken?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "리프레시 토큰을 입력해주세요.")

        return ApiResponse.success(
            message = "토큰이 재발급되었습니다.",
            data = tokenRefreshService.refresh(authorization, token),
        )
    }
}
