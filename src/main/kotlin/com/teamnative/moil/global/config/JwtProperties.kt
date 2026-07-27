package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moil.jwt")
data class JwtProperties(
    val secret: String = "moil-local-development-secret-key-must-be-at-least-32-bytes",
    val accessTokenExpiresIn: Long = 3600,
)
