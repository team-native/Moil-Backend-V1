package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.EmailVerificationStep
import com.teamnative.moil.domain.auth.dto.SendEmailCodeResponse
import com.teamnative.moil.domain.auth.dto.VerifyEmailCodeResponse
import com.teamnative.moil.domain.auth.mail.VerificationEmailTemplate
import com.teamnative.moil.domain.auth.model.EmailVerification
import com.teamnative.moil.domain.auth.model.VerifiedSignupSession
import com.teamnative.moil.domain.auth.repository.EmailVerificationRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.auth.repository.VerifiedSignupSessionRepository
import com.teamnative.moil.global.config.SmtpProperties
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
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
    private val userAccountRepository: UserAccountRepository,
    private val clock: Clock,
) {

    @Transactional
    fun sendCode(name: String?, email: String, step: EmailVerificationStep): SendEmailCodeResponse {
        val signupName = when (step) {
            EmailVerificationStep.SIGNUP -> name?.trim()?.takeIf { it.isNotBlank() }
                ?: throw ResponseStatusException(HttpStatusCode.valueOf(422), "이름을 입력하지 않았습니다.")
            EmailVerificationStep.RESET -> null
        }

        if (step == EmailVerificationStep.SIGNUP && userAccountRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.")
        }

        val verifyId = UUID.randomUUID().toString()
        val code = Random.nextInt(100000, 1000000).toString()
        val expiresAt = Instant.now(clock).plusSeconds(EMAIL_CODE_TTL_SECONDS)

        emailVerificationRepository.deleteByExpiresAtBefore(Instant.now(clock))
        emailVerificationRepository.save(
            EmailVerification(
                verifyId = verifyId,
                email = email,
                name = signupName,
                step = step,
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
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다.")
        }

        val sessionId = UUID.randomUUID().toString()
        verifiedSignupSessionRepository.deleteByExpiresAtBefore(Instant.now(clock))
        verifiedSignupSessionRepository.save(
            VerifiedSignupSession(
                sessionId = sessionId,
                email = verification.email,
                name = verification.name,
                step = verification.step,
                expiresAt = Instant.now(clock).plusSeconds(VERIFIED_SESSION_TTL_SECONDS),
            ),
        )
        emailVerificationRepository.delete(verification)

        return VerifyEmailCodeResponse(sessionId = sessionId)
    }

    @Transactional
    fun consumeVerifiedSession(
        sessionId: String,
        expectedStep: EmailVerificationStep,
        notFoundMessage: String = "회원가입 세션이 없거나 만료되었습니다.",
    ): VerifiedEmailSession {
        val session = verifiedSignupSessionRepository.findById(sessionId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage)

        if (session.expiresAt.isBefore(Instant.now(clock)) || session.step != expectedStep) {
            verifiedSignupSessionRepository.delete(session)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage)
        }

        verifiedSignupSessionRepository.delete(session)

        return VerifiedEmailSession(email = session.email, name = session.name)
    }

    data class VerifiedEmailSession(
        val email: String,
        val name: String?,
    )

    private fun sendMail(email: String, code: String) {
        val mimeMessage = mailSender.createMimeMessage()
        MimeMessageHelper(mimeMessage, false, "UTF-8").apply {
            setFrom(smtpProperties.from.ifBlank { smtpProperties.username })
            setTo(email)
            setSubject("Moil 이메일 인증번호")
            setText(VerificationEmailTemplate.build(code), true)
        }
        mailSender.send(mimeMessage)
    }

    companion object {
        private const val EMAIL_CODE_TTL_SECONDS = 5 * 60L
        private const val VERIFIED_SESSION_TTL_SECONDS = 5 * 60L
    }
}
