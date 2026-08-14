package com.teamnative.moil.domain.group.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GroupMemberResponse(
    val userId: Long,
    val nickname: String,
    val email: String,
    val role: String,
    val colorId: String?,
    val imagePath: String?,
    @get:JsonProperty("isMe")
    val isMe: Boolean,
)
