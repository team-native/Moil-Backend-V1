package com.teamnative.moil.domain.oauth.controller

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.domain.oauth.service.OauthService
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.view.RedirectView


@RestController
@RequestMapping("/oauth")
class OauthController (private val oauthService: OauthService) {
    @GetMapping("/{socialLoginType}")
    fun socialLogin(@PathVariable socialLoginType: SocialLoginType): RedirectView =
        RedirectView(oauthService.login(socialLoginType))

    @GetMapping("/{socialLoginType}/callback")
    fun callback(
        @PathVariable socialLoginType: SocialLoginType,
        @RequestParam code: String,
    ): ApiResponse<AuthTokenResponse> =
        ApiResponse.success(
            message = "소셜 로그인이 완료되었습니다.",
            data = oauthService.callback(socialLoginType, code),
        )

    @GetMapping("/apple")
    fun appleLogin(): RedirectView =
        RedirectView(oauthService.appleLogin())

    @PostMapping("/apple/callback")
    fun appleCallback(
        @RequestParam code: String,
        @RequestParam(required = false) user: String?,
    ): ApiResponse<AuthTokenResponse> =
        ApiResponse.success(
            message = "소셜 로그인이 완료되었습니다.",
            data = oauthService.appleCallback(code, user),
        )
}
