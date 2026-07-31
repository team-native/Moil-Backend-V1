package com.teamnative.moil.domain.group.dto

data class GroupDetailResponse(
    val groupId: Long,
    val name: String,
    val inviteCode: String,
    val memberCount: Long,
    val monthlyEventCount: Long,
    val myRole: String,
    val members: List<GroupDetailMemberResponse>,
)

data class GroupDetailMemberResponse(
    val userId: Long,
    val nickname: String,
    val role: String,
    val color: String,
)
