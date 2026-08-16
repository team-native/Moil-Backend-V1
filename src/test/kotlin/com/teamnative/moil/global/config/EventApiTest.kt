package com.teamnative.moil.global.config

import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.model.EventSharedMember
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class EventApiTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var eventSharedMemberRepository: EventSharedMemberRepository

    @Test
    fun `group events use month query and return notion calendar keys`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Calendar Group")
        val otherGroup = createGroup(name = "Other Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "GREEN",
                joinedAt = Instant.now(),
            ),
        )
        val event = eventRepository.save(
            Event(
                groupId = group.id,
                creatorId = session.userId,
                updaterId = session.userId,
                title = "Team Sync",
                memo = "Weekly planning",
                location = "Room A",
                startsAt = Instant.parse("2026-01-10T01:00:00Z"),
                endsAt = Instant.parse("2026-01-10T02:00:00Z"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        eventSharedMemberRepository.save(
            EventSharedMember(
                eventId = event.id,
                userId = session.userId,
                createdAt = Instant.now(),
            ),
        )
        createEvent(
            groupId = otherGroup.id,
            userId = session.userId,
            title = "Other Event",
            startsAt = Instant.parse("2026-01-10T01:00:00Z"),
            endsAt = Instant.parse("2026-01-10T02:00:00Z"),
        )

        mockMvc.perform(
            get("/groups/${group.id}/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("month", "2026-01"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].eventId").value(event.id))
            .andExpect(jsonPath("$.data[0].title").value("Team Sync"))
            .andExpect(jsonPath("$.data[0].startDate").value("2026-01-10 10:00"))
            .andExpect(jsonPath("$.data[0].endDate").value("2026-01-10 11:00"))
            .andExpect(jsonPath("$.data[0].isAllDay").doesNotExist())
            .andExpect(jsonPath("$.data[0].date").doesNotExist())
            .andExpect(jsonPath("$.data[0].startTime").doesNotExist())
            .andExpect(jsonPath("$.data[0].endTime").doesNotExist())
            .andExpect(jsonPath("$.data[0].location").value("Room A"))
            .andExpect(jsonPath("$.data[0].memo").value("Weekly planning"))
            .andExpect(jsonPath("$.data[0].members[0].userId").value(session.userId))
            .andExpect(jsonPath("$.data[0].members[0].nickname").value("Member"))
            .andExpect(jsonPath("$.data[0].members[0].colorId").value("GREEN"))
            .andExpect(jsonPath("$.data[0].startsAt").doesNotExist())
            .andExpect(jsonPath("$.data[0].endsAt").doesNotExist())
            .andExpect(jsonPath("$.data[1]").doesNotExist())
    }

    @Test
    fun `create event accepts notion request keys and returns eventId only`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Create Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "BLUE",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "groupId":${group.id},
                      "title":"Planning",
                      "startDate":"2026-02-03 09:30",
                      "endDate":"2026-02-03 10:30",
                      "location":"Room B",
                      "memo":"Bring agenda",
                      "sharedMemberIds":[${session.userId}]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.eventId").exists())
            .andExpect(jsonPath("$.data.groupId").doesNotExist())
            .andExpect(jsonPath("$.data.startsAt").doesNotExist())

        val event = eventRepository.findAll().first { it.title == "Planning" }
        val shares = eventSharedMemberRepository.findAllByEventId(event.id)

        assertEquals(group.id, event.groupId)
        assertEquals("Room B", event.location)
        assertEquals("Bring agenda", event.memo)
        assertEquals(listOf(session.userId), shares.map { it.userId })
    }

    @Test
    fun `create event stores null memo when request memo is blank`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Create Blank Memo Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "groupId":${group.id},
                      "title":"Planning",
                      "startDate":"2026-02-03 09:30",
                      "endDate":"2026-02-03 10:30",
                      "location":"Room B",
                      "memo":"",
                      "sharedMemberIds":[${session.userId}]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val event = eventRepository.findAll().first { it.title == "Planning" && it.groupId == group.id }

        assertEquals(null, event.memo)
    }

    @Test
    fun `create event requires shared members`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "groupId":1,
                      "title":"Planning",
                      "startDate":"2026-02-03 00:00",
                      "endDate":"2026-02-04 00:00",
                      "sharedMemberIds":[]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `create event requires startDate and endDate`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "groupId":1,
                      "title":"Planning",
                      "sharedMemberIds":[${session.userId}]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
    }

    @Test
    fun `event detail returns notion response keys`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Detail Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "YELLOW",
                joinedAt = Instant.now(),
            ),
        )
        val event = eventRepository.save(
            Event(
                groupId = group.id,
                creatorId = session.userId,
                updaterId = session.userId,
                title = "All Day",
                memo = "Remote friendly",
                location = "Online",
                startsAt = Instant.parse("2026-03-01T15:00:00Z"),
                endsAt = Instant.parse("2026-03-02T15:00:00Z"),
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        eventSharedMemberRepository.save(
            EventSharedMember(
                eventId = event.id,
                userId = session.userId,
                createdAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/events/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.eventId").value(event.id))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.title").value("All Day"))
            .andExpect(jsonPath("$.data.startDate").value("2026-03-02 00:00"))
            .andExpect(jsonPath("$.data.endDate").value("2026-03-03 00:00"))
            .andExpect(jsonPath("$.data.isAllDay").doesNotExist())
            .andExpect(jsonPath("$.data.date").doesNotExist())
            .andExpect(jsonPath("$.data.startTime").doesNotExist())
            .andExpect(jsonPath("$.data.endTime").doesNotExist())
            .andExpect(jsonPath("$.data.location").value("Online"))
            .andExpect(jsonPath("$.data.memo").value("Remote friendly"))
            .andExpect(jsonPath("$.data.members[0].colorId").value("YELLOW"))
            .andExpect(jsonPath("$.data.creatorId").doesNotExist())
            .andExpect(jsonPath("$.data.startsAt").doesNotExist())
    }

    @Test
    fun `update event uses patch and returns null data`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Update Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "GREEN",
                joinedAt = Instant.now(),
            ),
        )
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Before",
            startsAt = Instant.parse("2026-04-01T01:00:00Z"),
            endsAt = Instant.parse("2026-04-01T02:00:00Z"),
        ).copy(memo = "Before memo")
            .let { eventRepository.save(it) }

        mockMvc.perform(
            patch("/events/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"After",
                      "startDate":"2026-04-02 13:00",
                      "endDate":"2026-04-02 14:00",
                      "location":"Room C",
                      "memo":"Updated memo",
                      "sharedMemberIds":[${session.userId}]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        val updatedEvent = eventRepository.findById(event.id).orElseThrow()
        val shares = eventSharedMemberRepository.findAllByEventId(event.id)

        assertEquals("After", updatedEvent.title)
        assertEquals("Room C", updatedEvent.location)
        assertEquals("Updated memo", updatedEvent.memo)
        assertEquals(session.userId, updatedEvent.updaterId)
        assertEquals(listOf(session.userId), shares.map { it.userId })
    }

    @Test
    fun `update event clears memo when request memo is blank`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Clear Memo Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Before",
            startsAt = Instant.parse("2026-04-01T01:00:00Z"),
            endsAt = Instant.parse("2026-04-01T02:00:00Z"),
        ).copy(memo = "Existing memo")
            .let { eventRepository.save(it) }

        mockMvc.perform(
            patch("/events/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"After",
                      "startDate":"2026-04-02 13:00",
                      "endDate":"2026-04-02 14:00",
                      "location":"Room C",
                      "memo":"",
                      "sharedMemberIds":[${session.userId}]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val updatedEvent = eventRepository.findById(event.id).orElseThrow()

        assertEquals(null, updatedEvent.memo)
    }

    @Test
    fun `delete event uses notion path and clears shared members`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Delete Event Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Delete Me",
            startsAt = Instant.parse("2026-05-01T01:00:00Z"),
            endsAt = Instant.parse("2026-05-01T02:00:00Z"),
        )
        eventSharedMemberRepository.save(
            EventSharedMember(
                eventId = event.id,
                userId = session.userId,
                createdAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            delete("/events/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        assertFalse(eventRepository.existsById(event.id))
        assertEquals(emptyList<EventSharedMember>(), eventSharedMemberRepository.findAllByEventId(event.id))
    }
}
