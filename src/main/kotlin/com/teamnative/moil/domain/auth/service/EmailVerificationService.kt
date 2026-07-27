package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.SendEmailCodeResponse
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeResponse
import com.teamnative.moil.domain.auth.model.EmailVerification
import com.teamnative.moil.domain.auth.model.VerifiedSignupSession
import com.teamnative.moil.domain.auth.repository.EmailVerificationRepository
import com.teamnative.moil.domain.auth.repository.VerifiedSignupSessionRepository
import com.teamnative.moil.global.config.SmtpProperties
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

@Service
class EmailVerificationService(
    private val mailSender: JavaMailSender,
    private val smtpProperties: SmtpProperties,
    private val emailVerificationRepository: EmailVerificationRepository,
    private val verifiedSignupSessionRepository: VerifiedSignupSessionRepository,
    private val clock: Clock,
) {

    @Transactional
    fun sendCode(email: String): SendEmailCodeResponse {
        val verifyId = "ver_${UUID.randomUUID()}"
        val code = Random.nextInt(100000, 1000000).toString()
        val expiresAt = Instant.now(clock).plusSeconds(EMAIL_CODE_TTL_SECONDS)

        emailVerificationRepository.deleteByExpiresAtBefore(Instant.now(clock))
        emailVerificationRepository.save(
            EmailVerification(
                verifyId = verifyId,
                email = email,
                code = code,
                expiresAt = expiresAt,
            ),
        )

        if (smtpProperties.enabled) {
            sendMail(email, code)
        }

        return SendEmailCodeResponse(verifyId = verifyId)
    }

    @Transactional
    fun verifyCode(verifyId: String, code: String): VerifyEmailCodeResponse {
        val verification = emailVerificationRepository.findById(verifyId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없거나 만료되었습니다.")

        if (verification.expiresAt.isBefore(Instant.now(clock))) {
            emailVerificationRepository.delete(verification)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "인증 요청이 없거나 만료되었습니다.")
        }

        if (verification.code != code) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다.")
        }

        val sessionId = "sess_verify_${UUID.randomUUID()}"
        verifiedSignupSessionRepository.deleteByExpiresAtBefore(Instant.now(clock))
        verifiedSignupSessionRepository.save(
            VerifiedSignupSession(
                sessionId = sessionId,
                email = verification.email,
                expiresAt = Instant.now(clock).plusSeconds(VERIFIED_SESSION_TTL_SECONDS),
            ),
        )
        emailVerificationRepository.delete(verification)

        return VerifyEmailCodeResponse(sessionId = sessionId)
    }

    @Transactional
    fun consumeVerifiedSession(sessionId: String): String {
        val session = verifiedSignupSessionRepository.findById(sessionId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "회원가입 세션이 없거나 만료되었습니다.")

        if (session.expiresAt.isBefore(Instant.now(clock))) {
            verifiedSignupSessionRepository.delete(session)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "회원가입 세션이 없거나 만료되었습니다.")
        }

        verifiedSignupSessionRepository.delete(session)

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

    companion object {
        private const val EMAIL_CODE_TTL_SECONDS = 5 * 60L
        private const val VERIFIED_SESSION_TTL_SECONDS = 5 * 60L
    }
}
