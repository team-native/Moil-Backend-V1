package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.time.Clock
import java.util.Properties

@Configuration
@EnableConfigurationProperties(SmtpProperties::class)
class MailConfig {

    @Bean
    fun javaMailSender(properties: SmtpProperties): JavaMailSender =
        JavaMailSenderImpl().apply {
            host = properties.host
            port = properties.port
            username = properties.username
            password = properties.password
            javaMailProperties = Properties().apply {
                put("mail.smtp.auth", properties.username.isNotBlank().toString())
                put("mail.smtp.starttls.enable", "true")
            }
        }

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
