package com.teamnative.moil.global.config

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class GroupJoinLinkControllerTest : IntegrationTestSupport() {

    @Test
    fun `group join link redirects to app join activity`() {
        mockMvc.perform(get("/join/123"))
            .andExpect(status().isFound)
            .andExpect(header().string(HttpHeaders.LOCATION, "moil://join/123"))
    }

    @Test
    fun `group join link rejects non numeric group id`() {
        mockMvc.perform(get("/join/not-a-number"))
            .andExpect(status().isNotFound)
    }
}
