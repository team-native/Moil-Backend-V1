package com.teamnative.moil.domain.group.dto

data class JoinGroupResponse(
    val groupId: Long,
    val name: String,
    val myRole: String,
    val myNickname: String,
    val myColor: String?,
    val myImagePath: String?,
)
