package com.teamnative.moil.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moil.app-link")
data class AppLinkProperties(
    val groupJoinTemplate: String = "moil://join/{groupId}",
) {
    fun groupJoinUri(groupId: Long): String =
        groupJoinTemplate.replace("{groupId}", groupId.toString())
}
