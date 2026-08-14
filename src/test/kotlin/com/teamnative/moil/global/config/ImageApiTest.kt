package com.teamnative.moil.global.config

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
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
        val bytes = byteArrayOf(1, 2, 3, 4)
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

        mockMvc.perform(get(imagePath))
            .andExpect(status().isOk)
            .andExpect(content().contentType("image/png"))
            .andExpect { assertArrayEquals(bytes, it.response.contentAsByteArray) }
    }

    @Test
    fun `upload image replaces existing row without changing image path`() {
        val session = createLoginSession(password = "password")
        val first = MockMultipartFile("image", "profile.png", "image/png", byteArrayOf(1))
        val second = MockMultipartFile("image", "profile.webp", "image/webp", byteArrayOf(2, 3))

        val firstPath = uploadImage(session.accessToken, first)
        val secondPath = uploadImage(session.accessToken, second)
        val images = profileImageRepository.findAll().filter { it.userId == session.userId }

        assertEquals(firstPath, secondPath)
        assertEquals(1, images.size)
        assertArrayEquals(byteArrayOf(2, 3), images.single().image)
        assertEquals("image/webp", images.single().contentType)
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
}
