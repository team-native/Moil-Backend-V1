package com.teamnative.moil.global.logging

import java.time.Instant

data class AppLogDto(
    val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val event: LogEvent,
    val message: String,
    val traceId: String? = null,
    val server: ServerLogData? = null,
    val http: HttpLogData? = null,
    val error: ErrorLogData? = null,
)

data class ServerLogData(
    val application: String?,
    val port: String?,
    val profiles: List<String>,
)

data class HttpLogData(
    val method: String,
    val path: String,
    val query: String? = null,
    val status: Int? = null,
    val durationMs: Long? = null,
    val clientIp: String? = null,
    val request: PayloadLogData? = null,
    val response: PayloadLogData? = null,
)

data class PayloadLogData(
    val headers: Map<String, String> = emptyMap(),
    val body: Any? = null,
)

data class ErrorLogData(
    val type: String,
    val message: String?,
    val status: Int? = null,
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
}

enum class LogEvent {
    SERVER_STARTED,
    SERVER_STOPPED,
    EXTERNAL_REQUEST,
    RESPONSE_RETURNED,
    ERROR_OCCURRED,
}
