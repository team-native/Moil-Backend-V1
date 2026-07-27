package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.SendEmailCodeResponse
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeResponse
import com.teamnative.moil.global.config.SmtpProperties
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

@Service
class EmailVerificationService(
    private val mailSender: JavaMailSender,
    private val smtpProperties: SmtpProperties,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val verifications = ConcurrentHashMap<String, EmailVerification>()
    private val verifiedSessions = ConcurrentHashMap<String, VerifiedSession>()

    fun sendCode(email: String): SendEmailCodeResponse {
        val verifyId = "ver_${UUID.randomUUID()}"
        val code = Random.nextInt(100000, 1000000).toString()
        val expiresAt = Instant.now(clock).plusSeconds(EMAIL_CODE_TTL_SECONDS)

        verifications[verifyId] = EmailVerification(
            email = email,
            code = code,
            expiresAt = expiresAt,
        )

        if (smtpProperties.enabled) {
            sendMail(email, code)
        }

        return SendEmailCodeResponse(verifyId = verifyId)
    }

    fun verifyCode(verifyId: String, code: String): VerifyEmailCodeResponse {
        val verification = verifications[verifyId]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없거나 만료되었습니다.")

        if (verification.expiresAt.isBefore(Instant.now(clock))) {
            verifications.remove(verifyId)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없거나 만료되었습니다.")
        }

        if (verification.code != code) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다.")
        }

        val sessionId = "sess_verify_${UUID.randomUUID()}"
        verifiedSessions[sessionId] = VerifiedSession(
            email = verification.email,
            expiresAt = Instant.now(clock).plusSeconds(VERIFIED_SESSION_TTL_SECONDS),
        )
        verifications.remove(verifyId)

        return VerifyEmailCodeResponse(sessionId = sessionId)
    }

    fun consumeVerifiedSession(sessionId: String): String {
        val session = verifiedSessions[sessionId]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "회원가입 세션이 없거나 만료되었습니다.")

        if (session.expiresAt.isBefore(Instant.now(clock))) {
            verifiedSessions.remove(sessionId)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "회원가입 세션이 없거나 만료되었습니다.")
        }

        verifiedSessions.remove(sessionId)

        return session.email
    }

    private fun sendMail(email: String, code: String) {
        val message = SimpleMailMessage().apply {
            from = smtpProperties.from.ifBlank { smtpProperties.username }
            setTo(email)
            subject = "Moil 이메일 인증 코드"
            text = "인증 코드는 $code 입니다. 5분 안에 입력해주세요."
        }

        mailSender.send(message)
    }

    private data class EmailVerification(
        val email: String,
        val code: String,
        val expiresAt: Instant,
    )

    private data class VerifiedSession(
        val email: String,
        val expiresAt: Instant,
    )

    companion object {
        private const val EMAIL_CODE_TTL_SECONDS = 5 * 60L
        private const val VERIFIED_SESSION_TTL_SECONDS = 5 * 60L
    }
}
