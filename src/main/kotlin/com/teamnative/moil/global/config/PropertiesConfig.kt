package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(
    AppLinkProperties::class,
    JwtProperties::class,
    SmtpProperties::class,
)
class PropertiesConfig
