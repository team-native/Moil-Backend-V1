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
class GoogleOauth(
    private val socialOauthUserService: SocialOauthUserService,
    private val oauthProperties: OauthProperties,
) {
    private val restClient = RestClient.create()

    fun login(): String =
        UriComponentsBuilder
            .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
            .queryParam("client_id", requireSetting(oauthProperties.google.clientId, "GOOGLE_CLIENT_ID"))
            .queryParam("redirect_uri", oauthProperties.google.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email profile")
            .encode()
            .toUriString()

    fun callback(code: String): AuthTokenResponse {
        val token = requestToken(code)
        val profile = requestProfile(token.accessToken)

        return socialOauthUserService.loginOrCreate(
            provider = SocialLoginType.GOOGLE,
            providerUserId = profile.sub ?: throw invalidProfile(),
            email = profile.email ?: throw invalidProfile(),
            name = profile.name ?: profile.email.substringBefore("@"),
        )
    }

    private fun requestToken(code: String): GoogleTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", requireSetting(oauthProperties.google.clientId, "GOOGLE_CLIENT_ID"))
            add("client_secret", requireSetting(oauthProperties.google.clientSecret, "GOOGLE_CLIENT_SECRET"))
            add("code", code)
            add("grant_type", "authorization_code")
            add("redirect_uri", oauthProperties.google.redirectUri)
        }

        return restClient
            .post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(GoogleTokenResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google token response is empty.")
    }

    private fun requestProfile(accessToken: String): GoogleProfileResponse =
        restClient
            .get()
            .uri("https://www.googleapis.com/oauth2/v3/userinfo")
            .headers { it.setBearerAuth(accessToken) }
            .retrieve()
            .body(GoogleProfileResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google profile response is empty.")

    private fun requireSetting(value: String, name: String): String =
        value.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$name is required.")

    private fun invalidProfile(): ResponseStatusException =
        ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google profile does not include email.")

    data class GoogleTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
    )

    data class GoogleProfileResponse(
        val sub: String?,
        val email: String?,
        val name: String?,
    )
}
