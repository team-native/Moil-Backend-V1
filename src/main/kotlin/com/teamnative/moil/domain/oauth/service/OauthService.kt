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
    fun login(socialLoginType: SocialLoginType): String {
        return when (socialLoginType) {
            SocialLoginType.GOOGLE -> googleOauth.login()
            SocialLoginType.KAKAO -> kakaoOauth.login()
            SocialLoginType.APPLE -> appleOauth.login()
        }
    }

    fun callback(socialLoginType: SocialLoginType, code: String, user: String? = null): AuthTokenResponse {
        return when (socialLoginType) {
            SocialLoginType.GOOGLE -> googleOauth.callback(code)
            SocialLoginType.KAKAO -> kakaoOauth.callback(code)
            SocialLoginType.APPLE -> appleOauth.callback(code, user)
        }
    }

    fun appleLogin(): String = appleOauth.login()

    fun appleCallback(code: String, user: String?): AuthTokenResponse =
        appleOauth.callback(code, user)
}
