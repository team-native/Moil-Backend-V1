package com.teamnative.moil.domain.oauth.controller

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.oauth.dto.OauthTokenRequest
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.domain.oauth.service.OauthService
import com.teamnative.moil.global.config.AppLinkProperties
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView


@RestController
@RequestMapping("/oauth")
class OauthController(
    private val oauthService: OauthService,
    private val appLinkProperties: AppLinkProperties,
) {
    @GetMapping("/{socialLoginType}")
    fun socialLogin(
        @PathVariable socialLoginType: SocialLoginType,
        @RequestParam(required = false) state: String?,
    ): RedirectView =
        RedirectView(oauthService.login(socialLoginType, state))

    @GetMapping("/{socialLoginType}/callback")
    fun callback(
        @PathVariable socialLoginType: SocialLoginType,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<Void> =
        redirectToApp(socialLoginType, code, error, user = null, state)

    @PostMapping("/{socialLoginType}/token")
    fun token(
        @PathVariable socialLoginType: SocialLoginType,
        @RequestBody request: OauthTokenRequest,
    ): ApiResponse<AuthTokenResponse> =
        ApiResponse.success(
            message = "소셜 로그인이 완료되었습니다.",
            data = oauthService.callback(socialLoginType, request.code, request.user),
        )

    @GetMapping("/apple")
    fun appleLogin(@RequestParam(required = false) state: String?): RedirectView =
        RedirectView(oauthService.appleLogin(state))

    @PostMapping("/apple/callback")
    fun appleCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) user: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) state: String?,
    ): ResponseEntity<Void> =
        redirectToApp(SocialLoginType.APPLE, code, error, user, state)

    private fun redirectToApp(
        socialLoginType: SocialLoginType,
        code: String?,
        error: String?,
        user: String?,
        state: String?,
    ): ResponseEntity<Void> {
        val provider = socialLoginType.name.lowercase()
        val location = when {
            error != null -> appLinkProperties.oauthCallbackErrorUri(provider, error, state = state)
            code == null -> appLinkProperties.oauthCallbackErrorUri(provider, "missing_code", state = state)
            else -> runCatching {
                oauthService.callback(socialLoginType, code, user)
            }.fold(
                onSuccess = { token -> appLinkProperties.oauthCallbackTokenUri(provider, token, state) },
                onFailure = { exception ->
                    appLinkProperties.oauthCallbackErrorUri(
                        provider = provider,
                        error = "oauth_failed",
                        description = exception.message ?: "Social login failed.",
                        state = state,
                    )
                },
            )
        }

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, location)
            .build()
    }
}
