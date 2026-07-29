package com.teamnative.moil.global.logging

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AppLogger {
    private val logger = LoggerFactory.getLogger("moil.application")

    fun info(log: AppLogDto) {
        logger.info(toMessage(log.copy(level = LogLevel.INFO)))
    }

    fun warn(log: AppLogDto) {
        logger.warn(toMessage(log.copy(level = LogLevel.WARN)))
    }

    fun error(log: AppLogDto, exception: Throwable? = null) {
        val payload = toMessage(log.copy(level = LogLevel.ERROR))
        if (exception == null) {
            logger.error(payload)
        } else {
            logger.error(payload, exception)
        }
    }

    private fun toMessage(log: AppLogDto): String =
        buildList {
            add("[${log.event}]")
            add(log.message)
            log.traceId?.let { add("traceId=$it") }
            log.server?.let { server ->
                add("application=${server.application}")
                add("port=${server.port}")
                add("profiles=${server.profiles.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "default"}")
            }
            log.http?.let { http ->
                add("method=${http.method}")
                add("path=${http.path}")
                http.query?.let { add("query=$it") }
                http.status?.let { add("status=$it") }
                http.durationMs?.let { add("durationMs=$it") }
                http.clientIp?.let { add("clientIp=$it") }
                http.request?.let { payload ->
                    add("requestHeaders=${payload.headers}")
                    payload.body?.let { add("requestBody=$it") }
                }
                http.response?.let { payload ->
                    add("responseHeaders=${payload.headers}")
                    payload.body?.let { add("responseBody=$it") }
                }
            }
            log.error?.let { error ->
                add("errorType=${error.type}")
                error.status?.let { add("errorStatus=$it") }
                error.message?.let { add("errorMessage=$it") }
            }
        }.joinToString(" ")
}
