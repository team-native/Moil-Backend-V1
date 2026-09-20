package com.teamnative.moil.global.config

import com.teamnative.moil.domain.availability.repository.EventAvailabilityRepository
import com.teamnative.moil.domain.availability.repository.EventAvailabilitySlotRepository
import com.teamnative.moil.domain.event.model.EventSharedMember
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class AvailabilityApiTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var eventSharedMemberRepository: EventSharedMemberRepository

    @Autowired
    private lateinit var eventAvailabilityRepository: EventAvailabilityRepository

    @Autowired
    private lateinit var eventAvailabilitySlotRepository: EventAvailabilitySlotRepository

    @Test
    fun `availability api returns split slots with participant counts`() {
        val first = createLoginSession(password = "password")
        val second = createLoginSession(password = "password")
        val group = createGroup(name = "Availability Group")
        addMember(group.id, first.userId, "엄", "RED")
        addMember(group.id, second.userId, "아", "BLUE")
        val event = createEvent(
            groupId = group.id,
            userId = first.userId,
            title = "Schedule",
            startsAt = Instant.parse("2026-09-20T00:00:00Z"),
            endsAt = Instant.parse("2026-09-21T00:00:00Z"),
        )
        eventSharedMemberRepository.saveAll(
            listOf(
                EventSharedMember(eventId = event.id, userId = first.userId, createdAt = Instant.now()),
                EventSharedMember(eventId = event.id, userId = second.userId, createdAt = Instant.now()),
            ),
        )

        saveAvailability(event.id, first.accessToken, "12:31", "13:42")
        saveAvailability(event.id, second.accessToken, "13:10", "14:05")

        mockMvc.perform(
            get("/events/${event.id}/availability/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${first.accessToken}")
                .param("date", "2026-09-20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.participantCount").value(2))
            .andExpect(jsonPath("$.data.respondedCount").value(2))
            .andExpect(jsonPath("$.data.timeSlots[0].startTime").value("12:31"))
            .andExpect(jsonPath("$.data.timeSlots[0].endTime").value("13:10"))
            .andExpect(jsonPath("$.data.timeSlots[0].availableCount").value(1))
            .andExpect(jsonPath("$.data.timeSlots[1].startTime").value("13:10"))
            .andExpect(jsonPath("$.data.timeSlots[1].endTime").value("13:42"))
            .andExpect(jsonPath("$.data.timeSlots[1].availableCount").value(2))
            .andExpect(jsonPath("$.data.timeSlots[1].isAvailableForEveryone").value(true))
            .andExpect(jsonPath("$.data.timeSlots[2].startTime").value("13:42"))
            .andExpect(jsonPath("$.data.timeSlots[2].endTime").value("14:05"))
            .andExpect(jsonPath("$.data.timeSlots[2].availableCount").value(1))

        mockMvc.perform(
            get("/events/${event.id}/availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${first.accessToken}")
                .param("date", "2026-09-20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.members.length()").value(2))
            .andExpect(jsonPath("$.data.members[0].nickname").value("엄"))
            .andExpect(jsonPath("$.data.members[0].timeSlots[0].startTime").value("12:31"))
            .andExpect(jsonPath("$.data.members[1].nickname").value("아"))
            .andExpect(jsonPath("$.data.members[1].timeSlots[0].endTime").value("14:05"))
    }

    @Test
    fun `availability api replaces and deletes my response`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Availability Update Group")
        addMember(group.id, session.userId, "엄", "RED")
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Schedule",
            startsAt = Instant.parse("2026-09-20T00:00:00Z"),
            endsAt = Instant.parse("2026-09-21T00:00:00Z"),
        )
        eventSharedMemberRepository.save(
            EventSharedMember(eventId = event.id, userId = session.userId, createdAt = Instant.now()),
        )

        saveAvailability(event.id, session.accessToken, "10:00", "11:00")
        saveAvailability(event.id, session.accessToken, "12:31", "13:42")

        assertEquals(1, eventAvailabilityRepository.count())
        assertEquals(1, eventAvailabilitySlotRepository.count())

        mockMvc.perform(
            get("/events/${event.id}/availability/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("date", "2026-09-20"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.timeSlots[0].startTime").value("12:31"))
            .andExpect(jsonPath("$.data.timeSlots[0].endTime").value("13:42"))

        mockMvc.perform(
            delete("/events/${event.id}/availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("date", "2026-09-20"),
        )
            .andExpect(status().isOk)

        assertEquals(0, eventAvailabilityRepository.count())
        assertEquals(0, eventAvailabilitySlotRepository.count())
    }

    @Test
    fun `availability api rejects invalid time and non participant`() {
        val session = createLoginSession(password = "password")
        val outsider = createLoginSession(password = "password")
        val group = createGroup(name = "Availability Permission Group")
        addMember(group.id, session.userId, "엄", "RED")
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Schedule",
            startsAt = Instant.parse("2026-09-20T00:00:00Z"),
            endsAt = Instant.parse("2026-09-21T00:00:00Z"),
        )
        eventSharedMemberRepository.save(
            EventSharedMember(eventId = event.id, userId = session.userId, createdAt = Instant.now()),
        )

        mockMvc.perform(
            put("/events/${event.id}/availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-09-20","timeSlots":[{"startTime":"14:00","endTime":"13:00"}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))

        mockMvc.perform(
            get("/events/${event.id}/availability/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${outsider.accessToken}")
                .param("date", "2026-09-20"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
    }

    private fun saveAvailability(eventId: Long, accessToken: String, start: String, end: String) {
        mockMvc.perform(
            put("/events/$eventId/availability")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"date":"2026-09-20","timeSlots":[{"startTime":"$start","endTime":"$end"}]}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
    }

    private fun addMember(groupId: Long, userId: Long, nickname: String, color: String) {
        groupMemberRepository.save(
            GroupMember(
                groupId = groupId,
                userId = userId,
                role = GroupRole.MEMBER,
                nickname = nickname,
                color = color,
                joinedAt = Instant.now(),
            ),
        )
    }
}
