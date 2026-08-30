package com.teamnative.moil.domain.oauth.service

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import org.springframework.stereotype.Service

@Service
class OauthService(
    private val googleOauth: GoogleOauth,
    private val kakaoOauth: KakaoOauth,
    private val appleOauth: AppleOauth
) {
    fun login(socialLoginType: SocialLoginType, state: String? = null): String {
        return when (socialLoginType) {
            SocialLoginType.GOOGLE -> googleOauth.login(state)
            SocialLoginType.KAKAO -> kakaoOauth.login(state)
            SocialLoginType.APPLE -> appleOauth.login(state)
        }
    }

    fun callback(socialLoginType: SocialLoginType, code: String, user: String? = null): AuthTokenResponse {
        return when (socialLoginType) {
            SocialLoginType.GOOGLE -> googleOauth.callback(code)
            SocialLoginType.KAKAO -> kakaoOauth.callback(code)
            SocialLoginType.APPLE -> appleOauth.callback(code, user)
        }
    }

    fun appleLogin(state: String? = null): String = appleOauth.login(state)

    fun appleCallback(code: String, user: String?): AuthTokenResponse =
        appleOauth.callback(code, user)
}
