package com.teamnative.moil.domain.group.dto

data class CheckGroupInviteResponse(
    val groupId: Long,
    val name: String,
    val memberCount: Long,
    val inviteCode: String,
)
