package com.teamnative.moil.global.dto

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        fun empty(endpoint: String): ApiResponse<Nothing> =
            ApiResponse(
                success = true,
                status = 0,
                message = endpoint,
                data = null,
            )
    }
}
