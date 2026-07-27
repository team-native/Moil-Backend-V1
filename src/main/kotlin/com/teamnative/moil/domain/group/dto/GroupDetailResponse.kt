package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole

data class GroupDetailResponse(
    val groupId: Long,
    val name: String,
    val inviteCode: String,
    val role: GroupRole,
    val memberCount: Long,
    val notificationEnabled: Boolean,
)
