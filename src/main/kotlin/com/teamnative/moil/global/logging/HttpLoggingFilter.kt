package com.teamnative.moil.global.logging

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.Charset
import java.net.URLDecoder
import java.util.UUID

@Component
class HttpLoggingFilter(
    private val appLogger: AppLogger,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val wrappedRequest = ContentCachingRequestWrapper(request, MAX_BODY_LENGTH)
        val wrappedResponse = ContentCachingResponseWrapper(response)
        val startedAt = System.currentTimeMillis()
        wrappedRequest.setAttribute(TRACE_ID_ATTRIBUTE, traceId)

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } catch (exception: Exception) {
            logError(traceId, wrappedRequest, wrappedResponse, startedAt, exception)
            throw exception
        } finally {
            val durationMs = System.currentTimeMillis() - startedAt
            wrappedResponse.setHeader(TRACE_ID_HEADER, traceId)
            logRequest(traceId, wrappedRequest)
            logResponse(traceId, wrappedRequest, wrappedResponse, durationMs)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun logRequest(traceId: String, request: ContentCachingRequestWrapper) {
        appLogger.info(
            AppLogDto(
                level = LogLevel.INFO,
                event = LogEvent.EXTERNAL_REQUEST,
                message = "Incoming HTTP request.",
                traceId = traceId,
                http = HttpLogData(
                    method = request.method,
                    path = request.requestURI,
                    query = sanitizeQuery(request.queryString),
                    clientIp = clientIp(request),
                    request = PayloadLogData(
                        headers = loggableHeaders(request.headerNames.asSequence().associateWith { request.getHeader(it) }),
                        body = parseBody(request.contentAsByteArray, request.characterEncoding),
                    ),
                ),
            ),
        )
    }

    private fun logResponse(
        traceId: String,
        request: HttpServletRequest,
        response: ContentCachingResponseWrapper,
        durationMs: Long,
    ) {
        val status = response.status
        val log = AppLogDto(
            level = if (status >= 500) LogLevel.ERROR else LogLevel.INFO,
            event = LogEvent.RESPONSE_RETURNED,
            message = "HTTP response completed.",
            traceId = traceId,
            http = HttpLogData(
                method = request.method,
                path = request.requestURI,
                query = sanitizeQuery(request.queryString),
                status = status,
                durationMs = durationMs,
                clientIp = clientIp(request),
                response = PayloadLogData(
                    headers = loggableHeaders(response.headerNames.associateWith { response.getHeader(it).orEmpty() }),
                    body = parseBody(response.contentAsByteArray, response.characterEncoding),
                ),
            ),
        )

        if (status >= 500) {
            appLogger.error(log)
        } else {
            appLogger.info(log)
        }
    }

    private fun logError(
        traceId: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        startedAt: Long,
        exception: Exception,
    ) {
        appLogger.error(
            AppLogDto(
                level = LogLevel.ERROR,
                event = LogEvent.ERROR_OCCURRED,
                message = "Unhandled HTTP request error.",
                traceId = traceId,
                http = HttpLogData(
                    method = request.method,
                    path = request.requestURI,
                    query = sanitizeQuery(request.queryString),
                    status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    durationMs = System.currentTimeMillis() - startedAt,
                    clientIp = clientIp(request),
                ),
                error = ErrorLogData(
                    type = exception::class.qualifiedName ?: exception::class.simpleName.orEmpty(),
                    message = exception.message,
                    status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                ),
            ),
            exception,
        )
    }

    private fun loggableHeaders(headers: Map<String, String>): Map<String, String> =
        headers
            .filterKeys { key -> LOGGABLE_HEADERS.any { key.equals(it, ignoreCase = true) } }
            .mapValues { (key, value) ->
                if (LogSanitizer.sensitiveKeys.any { key.contains(it, ignoreCase = true) }) {
                    LogSanitizer.MASKED_VALUE
                } else {
                    value
                }
            }

    private fun parseBody(bytes: ByteArray, encoding: String?): Any? {
        if (bytes.isEmpty()) {
            return null
        }

        val charset = runCatching { Charset.forName(encoding ?: Charsets.UTF_8.name()) }.getOrDefault(Charsets.UTF_8)
        val body = String(bytes, charset).take(MAX_BODY_LENGTH)
        val parsed = runCatching { objectMapper.readValue(body, Any::class.java) }.getOrNull()

        return sanitizeBody(parsed ?: sanitizeRawBody(body))
    }

    private fun sanitizeBody(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> value.entries.associate { (key, entryValue) ->
                val keyText = key?.toString().orEmpty()
                keyText to if (LogSanitizer.sensitiveKeys.any { keyText.contains(it, ignoreCase = true) }) {
                    LogSanitizer.MASKED_VALUE
                } else {
                    sanitizeBody(entryValue)
                }
            }
            is List<*> -> value.map { sanitizeBody(it) }
            else -> value
        }

    private fun sanitizeQuery(query: String?): String? {
        if (query.isNullOrBlank()) {
            return query
        }

        return query
            .split("&")
            .joinToString("&") { part ->
                val key = part.substringBefore("=", part)
                val decodedKey = decodeUrlValue(key)

                if (LogSanitizer.sensitiveKeys.any { decodedKey.contains(it, ignoreCase = true) }) {
                    "$key=${LogSanitizer.MASKED_VALUE}"
                } else {
                    part
                }
            }
    }

    private fun sanitizeRawBody(body: String): String {
        if (LogSanitizer.sensitiveKeys.any { body.contains(it, ignoreCase = true) }) {
            return REDACTED_BODY_VALUE
        }

        return body
    }

    private fun decodeUrlValue(value: String): String =
        runCatching { URLDecoder.decode(value, Charsets.UTF_8) }.getOrDefault(value)

    private fun clientIp(request: HttpServletRequest): String? =
        request.getHeader("X-Forwarded-For")?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr

    companion object {
        private const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_ATTRIBUTE = "moil.traceId"
        private const val MAX_BODY_LENGTH = 2_000
        private const val REDACTED_BODY_VALUE = "[REDACTED_BODY_CONTAINS_SENSITIVE_FIELD]"
        private val LOGGABLE_HEADERS = listOf(
            "Content-Type",
            "Content-Length",
            "Accept",
            TRACE_ID_HEADER,
        )
    }
}
