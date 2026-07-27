package com.teamnative.moil.global.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

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
        mockMvc.perform(post("/auth/login"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").doesNotExist())

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
}
