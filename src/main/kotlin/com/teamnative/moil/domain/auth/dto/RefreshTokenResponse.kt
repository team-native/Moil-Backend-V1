package com.teamnative.moil.domain.auth.dto

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
)
