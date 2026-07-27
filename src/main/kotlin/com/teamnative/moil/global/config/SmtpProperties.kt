package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moil.smtp")
data class SmtpProperties(
    val enabled: Boolean = false,
    val host: String = "smtp.gmail.com",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val from: String = "",
)
