package com.teamnative.moil.domain.auth.mail

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationEmailTemplateTest {

    @Test
    fun `build places each digit in its own div`() {
        val html = VerificationEmailTemplate.build("123456")
        listOf("1", "2", "3", "4", "5", "6").forEach { digit ->
            assertTrue(html.contains("<div") && html.contains(">$digit<"))
        }
    }

    @Test
    fun `build generates exactly six digit boxes`() {
        val html = VerificationEmailTemplate.build("987654")
        assertEquals(6, "<div".toRegex().findAll(html).count())
    }

    @Test
    fun `build contains title and subtitle text`() {
        val html = VerificationEmailTemplate.build("000000")
        assertTrue(html.contains("이메일 인증"))
        assertTrue(html.contains("이메일 인증을 위한 인증번호 6자리입니다."))
    }

    @Test
    fun `build contains footer disclaimer`() {
        val html = VerificationEmailTemplate.build("000000")
        assertTrue(html.contains("본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다."))
    }

    @Test
    fun `build produces valid html skeleton`() {
        val html = VerificationEmailTemplate.build("123456")
        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("<body"))
        assertTrue(html.endsWith("</html>"))
    }
}
