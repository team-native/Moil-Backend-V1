package com.teamnative.moil.global.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest : IntegrationTestSupport() {

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
    fun `auth root is available`() {
        mockMvc.perform(get("/auth"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/auth"))
    }

    @Test
    fun `current api endpoints require login sessions`() {
        mockMvc.perform(post("/groups/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))

        mockMvc.perform(get("/events/1"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
    }

    @Test
    fun `current api endpoints are reachable with authentication`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

}
