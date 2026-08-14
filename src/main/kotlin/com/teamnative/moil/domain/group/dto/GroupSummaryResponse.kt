package com.teamnative.moil.domain.group.dto

data class GroupSummaryResponse(
    val groupId: Long,
    val name: String,
    val inviteCode: String,
    val myRole: String,
    val myNickname: String,
    val myColor: String?,
    val myImagePath: String?,
    val memberCount: Long,
)
