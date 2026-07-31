package com.teamnative.moil.domain.group.dto

data class CreateGroupResponse(
    val groupId: Long,
    val name: String,
    val inviteCode: String,
    val myRole: String,
)
