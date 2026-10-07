package com.teamnative.moil.global.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ProfileApiTest : IntegrationTestSupport() {

    @Test
    fun `profile endpoint returns readable Korean message`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            get("/auth/profile")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("프로필을 조회했습니다."))
            .andExpect(jsonPath("$.data.userId").value(session.userId))
    }
}
