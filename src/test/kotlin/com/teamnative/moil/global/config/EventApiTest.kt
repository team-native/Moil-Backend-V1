package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.model.VerifiedSignupSession
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class EventApiTest : IntegrationTestSupport() {

    fun `group calendar requires login session`() {
        mockMvc.perform(
            get("/events/groups/1/calendar")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-01-31T23:59:59Z"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group calendar returns not found group`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            get("/events/groups/999999/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-01-31T23:59:59Z"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group calendar rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")

        mockMvc.perform(
            get("/events/groups/${group.id}/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-01-31T23:59:59Z"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group calendar validates period format`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("from", "invalid")
                .param("to", "2026-01-31T23:59:59Z"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("기간 파라미터가 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group calendar validates period range`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("from", "2026-02-01T00:00:00Z")
                .param("to", "2026-01-01T00:00:00Z"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("조회 기간이 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group calendar returns overlapping events`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val otherGroup = createGroup(name = "다른 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val firstEvent = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "1월 회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )
        createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "2월 회의",
            startsAt = Instant.parse("2026-02-10T10:00:00Z"),
            endsAt = Instant.parse("2026-02-10T11:00:00Z"),
        )
        createEvent(
            groupId = otherGroup.id,
            userId = session.userId,
            title = "다른 그룹 회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/calendar")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-01-31T23:59:59Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 캘린더를 조회했습니다."))
            .andExpect(jsonPath("$.data[0].eventId").value(firstEvent.id))
            .andExpect(jsonPath("$.data[0].title").value("1월 회의"))
            .andExpect(jsonPath("$.data[0].startsAt").value("2026-01-10T10:00:00Z"))
            .andExpect(jsonPath("$.data[0].endsAt").value("2026-01-10T11:00:00Z"))
            .andExpect(jsonPath("$.data[1]").doesNotExist())
    }

    @Test
    fun `create group event requires login session`() {
        mockMvc.perform(
            post("/events/groups/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"회의","startsAt":"2026-01-10T10:00:00Z","endsAt":"2026-01-10T11:00:00Z"}
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group event validates required fields`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/events/groups/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"","startsAt":"2026-01-10T10:00:00Z","endsAt":"2026-01-10T11:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 제목을 입력해주세요."))

        mockMvc.perform(
            post("/events/groups/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"","endsAt":"2026-01-10T11:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("시작 시간을 입력해주세요."))

        mockMvc.perform(
            post("/events/groups/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"2026-01-10T10:00:00Z","endsAt":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("종료 시간을 입력해주세요."))
    }

    @Test
    fun `create group event validates content length`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/events/groups/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"${"가".repeat(101)}",
                      "startsAt":"2026-01-10T10:00:00Z",
                      "endsAt":"2026-01-10T11:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 제목은 100자 이하로 입력해주세요."))

        mockMvc.perform(
            post("/events/groups/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"회의",
                      "memo":"${"가".repeat(1001)}",
                      "startsAt":"2026-01-10T10:00:00Z",
                      "endsAt":"2026-01-10T11:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 메모는 1000자 이하로 입력해주세요."))
    }

    @Test
    fun `create group event returns not found group`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/events/groups/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"2026-01-10T10:00:00Z","endsAt":"2026-01-10T11:00:00Z"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group event rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")

        mockMvc.perform(
            post("/events/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"2026-01-10T10:00:00Z","endsAt":"2026-01-10T11:00:00Z"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group event validates event time format`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/events/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"invalid","endsAt":"2026-01-10T11:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("일정 시간 형식이 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group event validates event time range`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/events/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"회의","startsAt":"2026-01-10T11:00:00Z","endsAt":"2026-01-10T10:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("일정 시간 범위가 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group event saves event`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/events/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"회의",
                      "memo":"안건 정리",
                      "startsAt":"2026-01-10T10:00:00Z",
                      "endsAt":"2026-01-10T11:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 일정이 추가되었습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.title").value("회의"))
            .andExpect(jsonPath("$.data.memo").value("안건 정리"))
            .andExpect(jsonPath("$.data.startsAt").value("2026-01-10T10:00:00Z"))
            .andExpect(jsonPath("$.data.endsAt").value("2026-01-10T11:00:00Z"))
            .andExpect(jsonPath("$.data.creatorId").value(session.userId))
            .andExpect(jsonPath("$.data.updaterId").value(session.userId))
    }

    @Test
    fun `group event requires login session`() {
        mockMvc.perform(get("/events/groups/1/1"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group event returns not found group`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            get("/events/groups/999999/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group event rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group event returns not found event`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group event returns event detail`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val otherGroup = createGroup(name = "다른 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val event = eventRepository.save(
            Event(
                groupId = group.id,
                creatorId = session.userId,
                updaterId = session.userId,
                title = "회의",
                memo = "안건 정리",
                startsAt = Instant.parse("2026-01-10T10:00:00Z"),
                endsAt = Instant.parse("2026-01-10T11:00:00Z"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
            ),
        )
        createEvent(
            groupId = otherGroup.id,
            userId = session.userId,
            title = "다른 그룹 회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            get("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 일정을 조회했습니다."))
            .andExpect(jsonPath("$.data.eventId").value(event.id))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.title").value("회의"))
            .andExpect(jsonPath("$.data.memo").value("안건 정리"))
            .andExpect(jsonPath("$.data.startsAt").value("2026-01-10T10:00:00Z"))
            .andExpect(jsonPath("$.data.endsAt").value("2026-01-10T11:00:00Z"))
            .andExpect(jsonPath("$.data.creatorId").value(session.userId))
            .andExpect(jsonPath("$.data.updaterId").value(session.userId))
            .andExpect(jsonPath("$.data.createdAt").value("2026-01-01T00:00:00Z"))
            .andExpect(jsonPath("$.data.updatedAt").value("2026-01-02T00:00:00Z"))
    }

    @Test
    fun `update group event requires login session`() {
        mockMvc.perform(
            put("/events/groups/1/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T12:00:00Z","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event validates required fields`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            put("/events/groups/1/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"","startsAt":"2026-01-10T12:00:00Z","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 제목을 입력해주세요."))

        mockMvc.perform(
            put("/events/groups/1/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("시작 시간을 입력해주세요."))

        mockMvc.perform(
            put("/events/groups/1/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T12:00:00Z","endsAt":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("종료 시간을 입력해주세요."))
    }

    @Test
    fun `update group event validates content length`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            put("/events/groups/1/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"${"가".repeat(101)}",
                      "startsAt":"2026-01-10T12:00:00Z",
                      "endsAt":"2026-01-10T13:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 제목은 100자 이하로 입력해주세요."))

        mockMvc.perform(
            put("/events/groups/1/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"수정 회의",
                      "memo":"${"가".repeat(1001)}",
                      "startsAt":"2026-01-10T12:00:00Z",
                      "endsAt":"2026-01-10T13:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("일정 메모는 1000자 이하로 입력해주세요."))
    }

    @Test
    fun `update group event returns not found group`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            put("/events/groups/999999/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T12:00:00Z","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            put("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T12:00:00Z","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event returns not found event`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            put("/events/groups/${group.id}/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T12:00:00Z","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event validates event time format`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
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
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            put("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"invalid","endsAt":"2026-01-10T13:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("일정 시간 형식이 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event validates event time range`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
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
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            put("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"수정 회의","startsAt":"2026-01-10T13:00:00Z","endsAt":"2026-01-10T12:00:00Z"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("일정 시간 범위가 올바르지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group event saves event changes`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val event = eventRepository.save(
            Event(
                groupId = group.id,
                creatorId = 999999,
                updaterId = 999999,
                title = "회의",
                memo = "기존 메모",
                startsAt = Instant.parse("2026-01-10T10:00:00Z"),
                endsAt = Instant.parse("2026-01-10T11:00:00Z"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-02T00:00:00Z"),
            ),
        )

        mockMvc.perform(
            put("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "title":"수정 회의",
                      "memo":"수정 메모",
                      "startsAt":"2026-01-10T12:00:00Z",
                      "endsAt":"2026-01-10T13:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 일정이 수정되었습니다."))
            .andExpect(jsonPath("$.data.eventId").value(event.id))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.title").value("수정 회의"))
            .andExpect(jsonPath("$.data.memo").value("수정 메모"))
            .andExpect(jsonPath("$.data.startsAt").value("2026-01-10T12:00:00Z"))
            .andExpect(jsonPath("$.data.endsAt").value("2026-01-10T13:00:00Z"))
            .andExpect(jsonPath("$.data.creatorId").value(999999))
            .andExpect(jsonPath("$.data.updaterId").value(session.userId))
            .andExpect(jsonPath("$.data.createdAt").value("2026-01-01T00:00:00Z"))

        val updatedEvent = eventRepository.findByIdAndGroupId(event.id, group.id)
            ?: error("Updated event was not saved.")
        assertEquals("수정 회의", updatedEvent.title)
        assertEquals("수정 메모", updatedEvent.memo)
        assertEquals(session.userId, updatedEvent.updaterId)
        assertEquals(Instant.parse("2026-01-10T12:00:00Z"), updatedEvent.startsAt)
        assertEquals(Instant.parse("2026-01-10T13:00:00Z"), updatedEvent.endsAt)
    }

    @Test
    fun `delete group event requires login session`() {
        mockMvc.perform(delete("/events/groups/1/1"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete group event returns not found group`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            delete("/events/groups/999999/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete group event rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            delete("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete group event returns not found event`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            delete("/events/groups/${group.id}/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete group event removes event`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
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
            title = "회의",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            delete("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 일정이 삭제되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            get("/events/groups/${group.id}/${event.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."))
    }

}
