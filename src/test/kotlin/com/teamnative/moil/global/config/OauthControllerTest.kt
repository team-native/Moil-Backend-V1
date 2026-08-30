package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.domain.oauth.service.OauthService
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.mockito.Mockito.`when` as whenever

@SpringBootTest
@AutoConfigureMockMvc
class OauthControllerTest : IntegrationTestSupport() {

    @MockitoBean
    lateinit var oauthService: OauthService

    @Test
    fun `oauth callback exchanges authorization code and redirects tokens to app`() {
        whenever(oauthService.callback(SocialLoginType.GOOGLE, "google-auth-code", null))
            .thenReturn(authTokenResponse(accessToken = "app.access.jwt", refreshToken = "app.refresh.jwt"))

        mockMvc.perform(get("/oauth/google/callback").param("code", "google-auth-code"))
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/google/callback?accessToken=app.access.jwt&refreshToken=app.refresh.jwt",
                ),
            )
    }

    @Test
    fun `oauth callback includes state when redirecting tokens to app`() {
        whenever(oauthService.callback(SocialLoginType.GOOGLE, "google-auth-code", null))
            .thenReturn(authTokenResponse(accessToken = "app.access.jwt", refreshToken = "app.refresh.jwt"))

        mockMvc.perform(
            get("/oauth/google/callback")
                .param("code", "google-auth-code")
                .param("state", "android-state"),
        )
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/google/callback?state=android-state&accessToken=app.access.jwt&refreshToken=app.refresh.jwt",
                ),
            )
    }

    @Test
    fun `oauth callback redirects provider error to app without token exchange`() {
        mockMvc.perform(get("/oauth/kakao/callback").param("error", "access_denied"))
            .andExpect(status().isFound)
            .andExpect(header().string(HttpHeaders.LOCATION, "moil://oauth/kakao/callback?error=access_denied"))
    }

    @Test
    fun `oauth callback includes state when redirecting provider error to app`() {
        mockMvc.perform(
            get("/oauth/kakao/callback")
                .param("error", "access_denied")
                .param("state", "android-state"),
        )
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/kakao/callback?state=android-state&error=access_denied",
                ),
            )
    }

    @Test
    fun `oauth callback redirects token exchange failure to app error`() {
        whenever(oauthService.callback(SocialLoginType.KAKAO, "bad-code", null))
            .thenThrow(IllegalStateException("provider token exchange failed"))

        mockMvc.perform(get("/oauth/kakao/callback").param("code", "bad-code"))
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/kakao/callback?error=oauth_failed&error_description=provider%20token%20exchange%20failed",
                ),
            )
    }

    @Test
    fun `apple callback exchanges authorization code and redirects tokens to app`() {
        val appleUser = """{"name":{"firstName":"Moil","lastName":"User"}}"""
        whenever(oauthService.callback(SocialLoginType.APPLE, "apple-auth-code", appleUser))
            .thenReturn(authTokenResponse(accessToken = "apple.access.jwt", refreshToken = "apple.refresh.jwt"))

        mockMvc.perform(
            post("/oauth/apple/callback")
                .param("code", "apple-auth-code")
                .param("user", appleUser),
        )
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/apple/callback?accessToken=apple.access.jwt&refreshToken=apple.refresh.jwt",
                ),
            )
    }

    @Test
    fun `apple callback includes state when redirecting tokens to app`() {
        val appleUser = """{"name":{"firstName":"Moil","lastName":"User"}}"""
        whenever(oauthService.callback(SocialLoginType.APPLE, "apple-auth-code", appleUser))
            .thenReturn(authTokenResponse(accessToken = "apple.access.jwt", refreshToken = "apple.refresh.jwt"))

        mockMvc.perform(
            post("/oauth/apple/callback")
                .param("code", "apple-auth-code")
                .param("user", appleUser)
                .param("state", "android-state"),
        )
            .andExpect(status().isFound)
            .andExpect(
                header().string(
                    HttpHeaders.LOCATION,
                    "moil://oauth/apple/callback?state=android-state&accessToken=apple.access.jwt&refreshToken=apple.refresh.jwt",
                ),
            )
    }

    private fun authTokenResponse(
        accessToken: String,
        refreshToken: String,
    ): AuthTokenResponse =
        AuthTokenResponse(
            userId = 1L,
            name = "Moil User",
            email = "moil@example.com",
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = 3600,
        )
}
