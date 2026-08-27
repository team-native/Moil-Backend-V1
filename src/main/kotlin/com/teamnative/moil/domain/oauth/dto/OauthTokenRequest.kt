package com.teamnative.moil.domain.oauth.dto

data class OauthTokenRequest(
    val code: String,
    val user: String? = null,
)
