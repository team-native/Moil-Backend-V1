package com.teamnative.moil.global.logging

object LogSanitizer {
    const val MASKED_VALUE = "***"

    val sensitiveKeys = listOf(
        "authorization",
        "password",
        "pwd",
        "token",
        "secret",
        "cookie",
        "inviteCode",
    )
}
