package com.teamnative.moil.global.config

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ImageApiTest : IntegrationTestSupport() {

    @Test
    fun `upload image returns image path and image can be fetched`() {
        val session = createLoginSession(password = "password")
        val bytes = pngBytes(1, 2, 3, 4)
        val file = MockMultipartFile("image", "profile.png", "image/png", bytes)

        val uploadResult = mockMvc.perform(
            multipart("/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.imagePath").exists())
            .andReturn()

        val imagePath = imagePathFrom(uploadResult.response.contentAsString)

        // Not attached to any group yet, so it's still just a queued upload, not permanent
        // storage - but it's already previewable at its path.
        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)

        mockMvc.perform(get(imagePath))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/png"))
            .andExpect { assertArrayEquals(bytes, it.response.contentAsByteArray) }
    }

    @Test
    fun `re-uploading before attaching replaces the queued image`() {
        val session = createLoginSession(password = "password")
        val first = MockMultipartFile("image", "profile.png", "image/png", pngBytes(1))
        val second = MockMultipartFile("image", "profile.webp", "image/webp", webpBytes())

        val firstPath = uploadImage(session.accessToken, first)
        val secondPath = uploadImage(session.accessToken, second)

        assertNotEquals(firstPath, secondPath)
        // Neither upload was ever attached to a group, so nothing should exist in permanent
        // storage - the queue only ever holds the single most recent upload per user.
        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)

        mockMvc.perform(get(secondPath))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/webp"))

        // The first upload was discarded when the second one replaced it in the queue.
        mockMvc.perform(get(firstPath))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `upload image rejects unsupported content type`() {
        val session = createLoginSession(password = "password")
        val file = MockMultipartFile("image", "profile.gif", "image/gif", byteArrayOf(1))

        mockMvc.perform(
            multipart("/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `upload image rejects content that does not match a real image signature`() {
        val session = createLoginSession(password = "password")
        // Content-Type header claims PNG, but the bytes are not a PNG - the server must not
        // trust the declared header and should reject this rather than store and later serve it.
        val file = MockMultipartFile("image", "fake.png", "image/png", "not-an-image".toByteArray())

        mockMvc.perform(
            multipart("/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `upload image rejects missing file part`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            multipart("/images")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `fetch missing image returns not found`() {
        mockMvc.perform(get("/images/missing"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    private fun uploadImage(accessToken: String, file: MockMultipartFile): String {
        val result = mockMvc.perform(
            multipart("/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        return imagePathFrom(result.response.contentAsString)
    }

    private fun imagePathFrom(response: String): String =
        objectMapper.readTree(response).path("data").path("imagePath").asText()

    private fun pngBytes(vararg extra: Byte): ByteArray =
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + extra

    private fun webpBytes(): ByteArray =
        "RIFF".toByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBP".toByteArray()
}
