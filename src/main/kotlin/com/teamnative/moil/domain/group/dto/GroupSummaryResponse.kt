package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole

data class GroupSummaryResponse(
    val groupId: Long,
    val name: String,
    val role: GroupRole,
    val memberCount: Long,
    val notificationEnabled: Boolean,
)
