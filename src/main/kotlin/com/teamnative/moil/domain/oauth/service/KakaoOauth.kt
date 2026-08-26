package com.teamnative.moil.domain.oauth.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.global.config.OauthProperties
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@Service
class KakaoOauth(
    private val socialOauthUserService: SocialOauthUserService,
    private val oauthProperties: OauthProperties,
) {
    private val restClient = RestClient.create()

    fun login(): String =
        UriComponentsBuilder
            .fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("client_id", requireSetting(oauthProperties.kakao.clientId, "KAKAO_CLIENT_ID"))
            .queryParam("redirect_uri", oauthProperties.kakao.redirectUri)
            .queryParam("response_type", "code")
            .encode()
            .toUriString()

    fun callback(code: String): AuthTokenResponse {
        val token = requestToken(code)
        val profile = requestProfile(token.accessToken)
        val account = profile.kakaoAccount ?: throw invalidProfile()
        val email = account.email ?: throw invalidProfile()
        val name = account.profile?.nickname ?: email.substringBefore("@")

        return socialOauthUserService.loginOrCreate(
            provider = SocialLoginType.KAKAO,
            providerUserId = profile.id.toString(),
            email = email,
            name = name,
        )
    }

    private fun requestToken(code: String): KakaoTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", requireSetting(oauthProperties.kakao.clientId, "KAKAO_CLIENT_ID"))
            oauthProperties.kakao.clientSecret.takeIf { it.isNotBlank() }?.let {
                add("client_secret", it)
            }
            add("code", code)
            add("grant_type", "authorization_code")
            add("redirect_uri", oauthProperties.kakao.redirectUri)
        }

        return restClient
            .post()
            .uri("https://kauth.kakao.com/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(KakaoTokenResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao token response is empty.")
    }

    private fun requestProfile(accessToken: String): KakaoProfileResponse =
        restClient
            .get()
            .uri("https://kapi.kakao.com/v2/user/me")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(KakaoProfileResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao profile response is empty.")

    private fun requireSetting(value: String, name: String): String =
        value.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$name is required.")

    private fun invalidProfile(): ResponseStatusException =
        ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao profile does not include email.")

    data class KakaoTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
    )

    data class KakaoProfileResponse(
        val id: Long,
        @JsonProperty("kakao_account")
        val kakaoAccount: KakaoAccount?,
    )

    data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    data class KakaoProfile(
        val nickname: String?,
    )
}
