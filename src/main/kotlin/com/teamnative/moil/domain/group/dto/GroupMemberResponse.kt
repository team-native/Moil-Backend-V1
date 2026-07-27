package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole
import java.time.Instant

data class GroupMemberResponse(
    val memberId: Long,
    val userId: Long,
    val name: String,
    val email: String,
    val role: GroupRole,
    val joinedAt: Instant,
)
