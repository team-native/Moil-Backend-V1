package com.teamnative.moil.global.exception

import com.teamnative.moil.global.dto.ApiResponse
import com.teamnative.moil.global.logging.AppLogDto
import com.teamnative.moil.global.logging.AppLogger
import com.teamnative.moil.global.logging.ErrorLogData
import com.teamnative.moil.global.logging.HttpLogData
import com.teamnative.moil.global.logging.HttpLoggingFilter
import com.teamnative.moil.global.logging.LogSanitizer
import com.teamnative.moil.global.logging.LogEvent
import com.teamnative.moil.global.logging.LogLevel
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.net.URLDecoder

@RestControllerAdvice
class GlobalExceptionHandler(
    private val appLogger: AppLogger,
) {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = exception.bindingResult.fieldErrors
            .firstOrNull()
            ?.defaultMessage
            ?: "Invalid request."
        logException(exception, HttpStatus.BAD_REQUEST, message, request)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = "요청 형식이 올바르지 않습니다."
        logException(exception, HttpStatus.BAD_REQUEST, message, request)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), message))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        exception: NoResourceFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = "Resource not found."
        logException(exception, HttpStatus.NOT_FOUND, message, request)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), message))
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        exception: ResponseStatusException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val status = HttpStatus.valueOf(exception.statusCode.value())
        val message = exception.reason ?: status.reasonPhrase
        logException(exception, status, message, request)

        return ResponseEntity
            .status(status)
            .body(ApiResponse.failure(status.value(), message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        exception: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val message = "Unexpected server error."
        logException(exception, HttpStatus.INTERNAL_SERVER_ERROR, message, request)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), message))
    }

    private fun logException(
        exception: Exception,
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
    ) {
        val log = AppLogDto(
            level = if (status.is5xxServerError) LogLevel.ERROR else LogLevel.WARN,
            event = LogEvent.ERROR_OCCURRED,
            message = "Request error handled.",
            traceId = request.getAttribute(HttpLoggingFilter.TRACE_ID_ATTRIBUTE) as? String,
            http = HttpLogData(
                method = request.method,
                path = request.requestURI,
                query = sanitizeQuery(request.queryString),
                status = status.value(),
                clientIp = clientIp(request),
            ),
            error = ErrorLogData(
                type = exception::class.qualifiedName ?: exception::class.simpleName.orEmpty(),
                message = message,
                status = status.value(),
            ),
        )

        if (status.is5xxServerError) {
            appLogger.error(log, exception)
        } else {
            appLogger.warn(log)
        }
    }

    private fun clientIp(request: HttpServletRequest): String? =
        request.getHeader("X-Forwarded-For")?.substringBefore(",")?.trim()?.takeIf { it.isNotBlank() }
            ?: request.remoteAddr

    private fun sanitizeQuery(query: String?): String? {
        if (query.isNullOrBlank()) {
            return query
        }

        return query
            .split("&")
            .joinToString("&") { part ->
                val key = part.substringBefore("=", part)
                val decodedKey = runCatching { URLDecoder.decode(key, Charsets.UTF_8) }.getOrDefault(key)

                if (LogSanitizer.sensitiveKeys.any { decodedKey.contains(it, ignoreCase = true) }) {
                    "$key=${LogSanitizer.MASKED_VALUE}"
                } else {
                    part
                }
            }
    }
}
