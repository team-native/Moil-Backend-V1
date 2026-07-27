package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var loginSessionRepository: LoginSessionRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `health endpoint is permitted`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `api endpoint requires authentication`() {
        mockMvc.perform(get("/protected-resource"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `main api roots are available`() {
        mockMvc.perform(get("/auth"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/auth"))

        mockMvc.perform(get("/groups"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/groups"))

        mockMvc.perform(get("/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/events"))
    }

    @Test
    fun `nested api endpoints are not available yet`() {
        mockMvc.perform(get("/groups/me"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(get("/events/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `email verification code can be requested`() {
        mockMvc.perform(
            post("/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."))
            .andExpect(jsonPath("$.data.verifyId").exists())
    }

    @Test
    fun `unknown email verification code returns not found`() {
        mockMvc.perform(
            post("/auth/verify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"verifyId":"ver_unknown","code":"123456"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("인증 요청이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `signup confirm validates matching passwords`() {
        mockMvc.perform(
            post("/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password1","pwd":"password2"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `signup confirm requires verified session`() {
        mockMvc.perform(
            post("/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password","pwd":"password"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("회원가입 세션이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `login rejects invalid credentials`() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"unknown@example.com","password":"password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `reset password validates matching passwords`() {
        mockMvc.perform(
            post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password1","pwd":"password2"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `reset password requires verified session`() {
        mockMvc.perform(
            post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password","pwd":"password"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("비밀번호 초기화 세션이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password requires login session`() {
        mockMvc.perform(
            post("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"old-password","newpwd":"new-password","checkpwd":"new-password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password validates origin password`() {
        val session = createLoginSession(password = "old-password")

        mockMvc.perform(
            post("/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"wrong-password","newpwd":"new-password","checkpwd":"new-password"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("기존 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password validates new password confirmation`() {
        val session = createLoginSession(password = "old-password")

        mockMvc.perform(
            post("/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"old-password","newpwd":"new-password","checkpwd":"other-password"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("새 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `logout requires login session`() {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `logout removes current login session`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    @Test
    fun `refresh token requires login session`() {
        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"refresh_unknown"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `refresh token validates refresh token`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"wrong_refresh"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("리프레시 토큰이 유효하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `refresh token replaces login session tokens`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    @Test
    fun `delete account requires login session`() {
        mockMvc.perform(
            post("/auth/delete-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"password","leftData":true}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete account validates account credentials`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"wrong-password","leftData":true}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete account removes user and login sessions`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"password","leftData":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    private fun createLoginSession(password: String): TestLoginSession {
        val id = UUID.randomUUID()
        val email = "test-$id@example.com"
        val passwordHash = passwordEncoder.encode(password) ?: error("Password encoding failed.")
        val user = userAccountRepository.save(
            UserAccount(
                name = "test-user",
                email = email,
                passwordHash = passwordHash,
            ),
        )
        val accessToken = "access_$id"
        val refreshToken = "refresh_$id"

        loginSessionRepository.save(
            LoginSession(
                accessToken = accessToken,
                userId = user.id,
                refreshToken = refreshToken,
                expiresAt = Instant.now().plusSeconds(3600),
            ),
        )

        return TestLoginSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = email,
        )
    }

    private data class TestLoginSession(
        val accessToken: String,
        val refreshToken: String,
        val email: String,
    )
}
