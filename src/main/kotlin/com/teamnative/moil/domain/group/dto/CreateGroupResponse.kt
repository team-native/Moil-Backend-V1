package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole

data class CreateGroupResponse(
    val groupId: Long,
    val name: String,
    val inviteCode: String,
    val role: GroupRole,
)
