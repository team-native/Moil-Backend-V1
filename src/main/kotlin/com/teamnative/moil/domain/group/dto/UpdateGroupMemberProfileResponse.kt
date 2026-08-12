package com.teamnative.moil.domain.group.dto

data class UpdateGroupMemberProfileResponse(
    val groupId: Long,
    val userId: Long,
    val nickname: String,
    val colorId: String,
)
