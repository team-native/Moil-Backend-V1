package com.teamnative.moil.global.logging

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AppLogger(
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger("moil.application")

    fun info(log: AppLogDto) {
        logger.info(toJson(log.copy(level = LogLevel.INFO)))
    }

    fun warn(log: AppLogDto) {
        logger.warn(toJson(log.copy(level = LogLevel.WARN)))
    }

    fun error(log: AppLogDto, exception: Throwable? = null) {
        val payload = toJson(log.copy(level = LogLevel.ERROR))
        if (exception == null) {
            logger.error(payload)
        } else {
            logger.error(payload, exception)
        }
    }

    private fun toJson(log: AppLogDto): String =
        runCatching { objectMapper.writeValueAsString(log) }
            .getOrElse { """{"level":"${log.level}","event":"${log.event}","message":"${log.message}"}""" }
}
