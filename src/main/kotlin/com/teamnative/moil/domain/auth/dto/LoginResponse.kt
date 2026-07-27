package com.teamnative.moil.domain.auth.dto

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val username: String,
)
