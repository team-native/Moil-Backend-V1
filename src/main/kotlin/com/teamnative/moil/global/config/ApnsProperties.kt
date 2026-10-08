package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moil.apns")
data class ApnsProperties(
    val enabled: Boolean = false,
    val teamId: String = "",
    val keyId: String = "",
    val privateKey: String = "",
    val bundleId: String = "com.teamnative.Moil",
    val environment: String = "sandbox",
)
