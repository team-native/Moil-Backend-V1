package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole

data class UpdateGroupMemberRoleResponse(
    val groupId: Long,
    val memberId: Long,
    val role: GroupRole,
)
