package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.global.config.IntegrationTestSupport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc

@SpringBootTest
@AutoConfigureMockMvc
class ProfileServiceTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var profileService: ProfileService

    @Test
    fun `update trims and saves profile name`() {
        val session = createLoginSession(password = "password")
        val user = userAccountRepository.findById(session.userId).orElseThrow()

        val response = profileService.update(user, "  new-name  ")
        val savedUser = userAccountRepository.findById(session.userId).orElseThrow()

        assertEquals("new-name", response.name)
        assertEquals("new-name", savedUser.name)
        assertEquals(session.userId, response.userId)
        assertEquals(session.email, response.email)
    }
}
