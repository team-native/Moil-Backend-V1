package com.teamnative.moil.domain.oauth.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
class AppleOauth(
    private val socialOauthUserService: SocialOauthUserService,
    private val appleClientSecretGenerator: AppleClientSecretGenerator,
    private val appleIdTokenVerifier: AppleIdTokenVerifier,
    private val objectMapper: ObjectMapper,
    private val oauthProperties: OauthProperties,
) {
    private val restClient = RestClient.create()

    fun login(): String =
        UriComponentsBuilder
            .fromUriString("https://appleid.apple.com/auth/authorize")
            .queryParam("client_id", requireSetting(oauthProperties.apple.clientId, "APPLE_CLIENT_ID"))
            .queryParam("redirect_uri", oauthProperties.apple.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("response_mode", "form_post")
            .queryParam("scope", "name email")
            .encode()
            .toUriString()

    fun callback(code: String, user: String?): AuthTokenResponse {
        val token = requestToken(code)
        val idToken = appleIdTokenVerifier.verify(token.idToken)
        val email = idToken.email ?: throw invalidProfile()
        val appleUser = parseAppleUser(user)
        val name = appleUser?.fullName()?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")

        return socialOauthUserService.loginOrCreate(
            provider = SocialLoginType.APPLE,
            providerUserId = idToken.subject,
            email = email,
            name = name,
        )
    }

    private fun requestToken(code: String): AppleTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("client_id", requireSetting(oauthProperties.apple.clientId, "APPLE_CLIENT_ID"))
            add("client_secret", appleClientSecretGenerator.generate())
            add("code", code)
            add("grant_type", "authorization_code")
            add("redirect_uri", oauthProperties.apple.redirectUri)
        }

        return restClient
            .post()
            .uri("https://appleid.apple.com/auth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(AppleTokenResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple token response is empty.")
    }

    private fun parseAppleUser(user: String?): AppleUser? =
        user?.takeIf { it.isNotBlank() }?.let {
            runCatching { objectMapper.readValue<AppleUser>(it) }.getOrNull()
        }

    private fun requireSetting(value: String, name: String): String =
        value.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "$name is required.")

    private fun invalidProfile(): ResponseStatusException =
        ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple profile does not include email.")

    data class AppleTokenResponse(
        @JsonProperty("id_token")
        val idToken: String,
    )

    data class AppleUser(
        val name: AppleName?,
    ) {
        fun fullName(): String =
            listOfNotNull(name?.firstName, name?.lastName).joinToString(" ")
    }

    data class AppleName(
        val firstName: String?,
        val lastName: String?,
    )
}
