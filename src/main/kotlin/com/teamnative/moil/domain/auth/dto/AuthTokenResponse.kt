package com.teamnative.moil.domain.auth.dto

data class AuthTokenResponse(
    val userId: Long,
    val name: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
)
