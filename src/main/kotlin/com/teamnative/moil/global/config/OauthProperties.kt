package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moil.oauth")
data class OauthProperties(
    val google: Provider = Provider(
        redirectUri = "http://localhost:6500/oauth/google/callback",
    ),
    val kakao: Provider = Provider(
        redirectUri = "http://localhost:6500/oauth/kakao/callback",
    ),
    val apple: Apple = Apple(
        redirectUri = "http://localhost:6500/oauth/apple/callback",
    ),
) {
    data class Provider(
        val clientId: String = "",
        val clientSecret: String = "",
        val redirectUri: String = "",
    )

    data class Apple(
        val clientId: String = "",
        val clientSecret: String = "",
        val redirectUri: String = "",
        val teamId: String = "",
        val keyId: String = "",
        val privateKey: String = "",
    )
}
