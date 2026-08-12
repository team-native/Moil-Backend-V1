package com.teamnative.moil.domain.auth.dto

data class ProfileResponse(
    val userId: Long,
    val name: String,
    val email: String,
)
