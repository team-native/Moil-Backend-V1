package com.teamnative.moil.global.dto

data class ApiResponse<T>(
    val success: Boolean,
    val status: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> success(message: String, data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                status = 0,
                message = message,
                data = data,
            )

        fun empty(endpoint: String): ApiResponse<Nothing> =
            ApiResponse(
                success = true,
                status = 0,
                message = endpoint,
                data = null,
            )

        fun failure(status: Int, message: String): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                status = status,
                message = message,
                data = null,
            )
    }
}
