package com.teamnative.moil.global.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class OauthControllerTest : IntegrationTestSupport() {

    @Test
    fun `oauth callback redirects authorization code to app`() {
        mockMvc.perform(get("/oauth/google/callback").param("code", "google-auth-code"))
            .andExpect(status().isFound)
            .andExpect(header().string(HttpHeaders.LOCATION, "moil://oauth/google/callback?code=google-auth-code"))
    }

    @Test
    fun `oauth callback redirects provider error to app`() {
        mockMvc.perform(get("/oauth/kakao/callback").param("error", "access_denied"))
            .andExpect(status().isFound)
            .andExpect(header().string(HttpHeaders.LOCATION, "moil://oauth/kakao/callback?error=access_denied"))
    }

    @Test
    fun `apple callback redirects authorization code to app`() {
        mockMvc.perform(
            post("/oauth/apple/callback")
                .param("code", "apple-auth-code")
                .param("user", """{"name":{"firstName":"Moil","lastName":"User"}}"""),
        )
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/apple/callback?code=apple-auth-code&user=%7B%22name%22:%7B%22firstName%22:%22Moil%22,%22lastName%22:%22User%22%7D%7D",
                ),
            )
    }
}
