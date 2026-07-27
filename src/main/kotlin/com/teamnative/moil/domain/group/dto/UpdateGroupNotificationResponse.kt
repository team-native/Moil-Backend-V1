package com.teamnative.moil.domain.group.dto

data class UpdateGroupNotificationResponse(
    val groupId: Long,
    val notificationEnabled: Boolean,
)
