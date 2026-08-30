package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.util.UriComponentsBuilder

@ConfigurationProperties(prefix = "moil.app-link")
data class AppLinkProperties(
    val groupJoinTemplate: String = "moil://join/{groupId}",
    val oauthCallbackTemplate: String = "moil://oauth/{provider}/callback",
) {
    fun groupJoinUri(groupId: Long): String =
        groupJoinTemplate.replace("{groupId}", groupId.toString())

    fun oauthCallbackUri(provider: String, code: String, user: String? = null): String {
        val builder = UriComponentsBuilder
            .fromUriString(oauthCallbackTemplate.replace("{provider}", provider))
            .queryParam("code", code)

        user?.let { builder.queryParam("user", it) }

        return builder
            .build()
            .encode()
            .toUriString()
    }

    fun oauthCallbackTokenUri(provider: String, token: AuthTokenResponse): String =
        baseOauthCallbackUri(provider)
            .queryParam("accessToken", token.accessToken)
            .queryParam("refreshToken", token.refreshToken)
            .build()
            .encode()
            .toUriString()

    fun oauthCallbackErrorUri(provider: String, error: String, description: String? = null): String {
        val builder = baseOauthCallbackUri(provider)
            .queryParam("error", error)

        description?.takeIf { it.isNotBlank() }?.let {
            builder.queryParam("error_description", it)
        }

        return builder
            .build()
            .encode()
            .toUriString()
    }

    private fun baseOauthCallbackUri(provider: String): UriComponentsBuilder =
        UriComponentsBuilder
            .fromUriString(oauthCallbackTemplate.replace("{provider}", provider))
}
