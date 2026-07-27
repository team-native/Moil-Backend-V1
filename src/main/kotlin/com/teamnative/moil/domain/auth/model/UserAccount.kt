package com.teamnative.moil.domain.auth.model

data class UserAccount(
    val id: Long,
    val name: String,
    val email: String,
    val password: String,
)
