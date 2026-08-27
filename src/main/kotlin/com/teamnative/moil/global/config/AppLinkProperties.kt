package com.teamnative.moil.global.config

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

    fun oauthCallbackErrorUri(provider: String, error: String): String =
        UriComponentsBuilder
            .fromUriString(oauthCallbackTemplate.replace("{provider}", provider))
            .queryParam("error", error)
            .build()
            .encode()
            .toUriString()
}
