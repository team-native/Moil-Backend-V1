package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.repository.UserDeviceTokenRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class DeviceTokenApiTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var userDeviceTokenRepository: UserDeviceTokenRepository

    @Test
    fun `device token registration is idempotent and updates platform`() {
        val session = createLoginSession(password = "password")
        val token = "apns-token-123"

        mockMvc.perform(
            put("/users/me/device-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","platform":"IOS"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.token").value(token))
            .andExpect(jsonPath("$.data.platform").value("IOS"))

        mockMvc.perform(
            put("/users/me/device-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","platform":"ANDROID"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.platform").value("ANDROID"))

        val saved = userDeviceTokenRepository.findByToken(token)
        assertEquals(session.userId, saved?.userId)
        assertEquals("ANDROID", saved?.platform?.name)
        assertEquals(1, userDeviceTokenRepository.findAll().count { it.token == token })
    }

    @Test
    fun `device token can be deleted`() {
        val session = createLoginSession(password = "password")
        val token = "apns-token-to-delete"

        mockMvc.perform(
            put("/users/me/device-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token","platform":"IOS"}"""),
        )
            .andExpect(status().isOk)

        mockMvc.perform(
            delete("/users/me/device-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"$token"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        assertTrue(userDeviceTokenRepository.findByToken(token) == null)
    }

    @Test
    fun `device token endpoints require the bearer token`() {
        mockMvc.perform(
            put("/users/me/device-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"apns-token-unauthorized","platform":"IOS"}"""),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `device token registration rejects unsupported platform`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            put("/users/me/device-token")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"token":"device-token-invalid-platform","platform":"WEB"}"""),
        )
            .andExpect(status().isBadRequest)
    }
}
