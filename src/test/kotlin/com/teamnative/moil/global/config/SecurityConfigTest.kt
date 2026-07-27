package com.teamnative.moil.global.config

import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.auth.service.JwtProvider
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import com.teamnative.moil.global.config.JwtProperties
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var loginSessionRepository: LoginSessionRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtProvider: JwtProvider

    @Autowired
    private lateinit var jwtProperties: JwtProperties

    @Test
    fun `health endpoint is permitted`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
    }

    @Test
    fun `api endpoint requires authentication`() {
        mockMvc.perform(get("/protected-resource"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `main api roots are available`() {
        mockMvc.perform(get("/auth"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/auth"))

        mockMvc.perform(get("/events"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("/events"))
    }

    @Test
    fun `nested api endpoints are not available yet`() {
        mockMvc.perform(get("/groups/me"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(get("/events/1"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `my groups requires login session`() {
        mockMvc.perform(get("/groups"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `my groups returns joined group list`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.OWNER,
                notificationEnabled = false,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("내 그룹 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data[0].groupId").value(group.id))
            .andExpect(jsonPath("$.data[0].name").value("스터디 그룹"))
            .andExpect(jsonPath("$.data[0].role").value("OWNER"))
            .andExpect(jsonPath("$.data[0].memberCount").value(1))
            .andExpect(jsonPath("$.data[0].notificationEnabled").value(false))
    }

    @Test
    fun `group detail requires login session`() {
        mockMvc.perform(get("/groups/1"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group detail returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            get("/groups/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group detail rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")

        mockMvc.perform(
            get("/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group detail returns group information`() {
        val session = createLoginSession(password = "password")
        val otherSession = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.saveAll(
            listOf(
                GroupMember(
                    groupId = group.id,
                    userId = session.userId,
                    role = GroupRole.ADMIN,
                    notificationEnabled = false,
                    joinedAt = Instant.now(),
                ),
                GroupMember(
                    groupId = group.id,
                    userId = otherSession.userId,
                    role = GroupRole.MEMBER,
                    joinedAt = Instant.now(),
                ),
            ),
        )

        mockMvc.perform(
            get("/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 정보를 조회했습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("스터디 그룹"))
            .andExpect(jsonPath("$.data.inviteCode").value(group.inviteCode))
            .andExpect(jsonPath("$.data.role").value("ADMIN"))
            .andExpect(jsonPath("$.data.memberCount").value(2))
            .andExpect(jsonPath("$.data.notificationEnabled").value(false))
    }

    @Test
    fun `group members requires login session`() {
        mockMvc.perform(get("/groups/1/members"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group members returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            get("/groups/999999/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group members rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")

        mockMvc.perform(
            get("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `group members returns member list`() {
        val ownerSession = createLoginSession(password = "password")
        val memberSession = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        val ownerMember = groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = ownerSession.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now().minusSeconds(10),
            ),
        )
        val member = groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = memberSession.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            get("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 멤버 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data[0].memberId").value(ownerMember.id))
            .andExpect(jsonPath("$.data[0].userId").value(ownerSession.userId))
            .andExpect(jsonPath("$.data[0].name").value("test-user"))
            .andExpect(jsonPath("$.data[0].email").value(ownerSession.email))
            .andExpect(jsonPath("$.data[0].role").value("OWNER"))
            .andExpect(jsonPath("$.data[0].joinedAt").exists())
            .andExpect(jsonPath("$.data[1].memberId").value(member.id))
            .andExpect(jsonPath("$.data[1].userId").value(memberSession.userId))
            .andExpect(jsonPath("$.data[1].email").value(memberSession.email))
            .andExpect(jsonPath("$.data[1].role").value("MEMBER"))
    }

    @Test
    fun `update group notification requires login session`() {
        mockMvc.perform(
            post("/groups/1/notification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEnabled":false}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group notification requires setting value`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/1/notification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("알림 설정 여부를 입력해주세요."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group notification returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/999999/notification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEnabled":false}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group notification rejects non member`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")

        mockMvc.perform(
            post("/groups/${group.id}/notification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEnabled":false}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 멤버가 아닙니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group notification changes member setting`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                notificationEnabled = true,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/${group.id}/notification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"notificationEnabled":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 알림 설정이 변경되었습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.notificationEnabled").value(false))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")

        assert(!member.notificationEnabled)
    }

    @Test
    fun `update group name requires login session`() {
        mockMvc.perform(
            post("/groups/1/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"새 그룹"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group name validates name`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/1/name")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("그룹 이름을 입력해주세요."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group name returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/999999/name")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"새 그룹"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("그룹을 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group name rejects member role`() {
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
            post("/groups/${group.id}/name")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"새 그룹"}"""),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("그룹 권한이 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `update group name changes group name`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.ADMIN,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/${group.id}/name")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"새 그룹"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹 이름이 변경되었습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("새 그룹"))

        val updatedGroup = groupRepository.findById(group.id).orElseThrow()

        assert(updatedGroup.name == "새 그룹")
    }

    @Test
    fun `create group requires login session`() {
        mockMvc.perform(
            post("/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"스터디 그룹"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group validates name`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("그룹 이름을 입력해주세요."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `create group registers owner member`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"스터디 그룹"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹이 생성되었습니다."))
            .andExpect(jsonPath("$.data.groupId").exists())
            .andExpect(jsonPath("$.data.name").value("스터디 그룹"))
            .andExpect(jsonPath("$.data.inviteCode").exists())
            .andExpect(jsonPath("$.data.role").value("OWNER"))

        val group = groupRepository.findAll().first { it.name == "스터디 그룹" }
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Created owner member not found.")

        assert(member.role == GroupRole.OWNER)
    }

    @Test
    fun `check group invite requires invite code`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/invite/check")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("초대 코드를 입력해주세요."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `check group invite requires login session`() {
        mockMvc.perform(
            post("/groups/invite/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"invite-code"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `check group invite returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/invite/check")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"unknown"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("초대 코드를 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `check group invite rejects already joined member`() {
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
            post("/groups/invite/check")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("이미 참가한 그룹입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `check group invite returns group information`() {
        val session = createLoginSession(password = "password")
        val otherSession = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = otherSession.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/invite/check")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("참가 가능한 그룹입니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("스터디 그룹"))
            .andExpect(jsonPath("$.data.memberCount").value(1))
    }

    @Test
    fun `join group requires login session`() {
        mockMvc.perform(
            post("/groups/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"invite-code"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `join group requires invite code`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":""}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("초대 코드를 입력해주세요."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `join group returns not found`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"unknown"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("초대 코드를 찾을 수 없습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `join group rejects already joined member`() {
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
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}"}"""),
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("이미 참가한 그룹입니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `join group registers member`() {
        val session = createLoginSession(password = "password")
        val ownerSession = createLoginSession(password = "password")
        val group = createGroup(name = "스터디 그룹")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = ownerSession.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("그룹에 참가했습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("스터디 그룹"))
            .andExpect(jsonPath("$.data.role").value("MEMBER"))
            .andExpect(jsonPath("$.data.memberCount").value(2))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Joined member not found.")

        assert(member.role == GroupRole.MEMBER)
    }

    @Test
    fun `email verification code can be requested`() {
        mockMvc.perform(
            post("/auth/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("인증 코드가 발송되었습니다."))
            .andExpect(jsonPath("$.data.verifyId").exists())
    }

    @Test
    fun `unknown email verification code returns not found`() {
        mockMvc.perform(
            post("/auth/verify-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"verifyId":"ver_unknown","code":"123456"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("인증 요청이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `signup confirm validates matching passwords`() {
        mockMvc.perform(
            post("/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password1","pwd":"password2"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `signup confirm requires verified session`() {
        mockMvc.perform(
            post("/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password","pwd":"password"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("회원가입 세션이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `login rejects invalid credentials`() {
        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"unknown@example.com","password":"password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `reset password validates matching passwords`() {
        mockMvc.perform(
            post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password1","pwd":"password2"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `reset password requires verified session`() {
        mockMvc.perform(
            post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sessionId":"sess_verify_unknown","password":"password","pwd":"password"}"""),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("비밀번호 초기화 세션이 없거나 만료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password requires login session`() {
        mockMvc.perform(
            post("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"old-password","newpwd":"new-password","checkpwd":"new-password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password validates origin password`() {
        val session = createLoginSession(password = "old-password")

        mockMvc.perform(
            post("/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"wrong-password","newpwd":"new-password","checkpwd":"new-password"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("기존 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `change password validates new password confirmation`() {
        val session = createLoginSession(password = "old-password")

        mockMvc.perform(
            post("/auth/change-password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"origin":"old-password","newpwd":"new-password","checkpwd":"other-password"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("새 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `logout requires login session`() {
        mockMvc.perform(post("/auth/logout"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `logout removes current login session`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    @Test
    fun `refresh token requires login session`() {
        mockMvc.perform(
            post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"refresh_unknown"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `refresh token validates refresh token`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"wrong_refresh"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("리프레시 토큰이 유효하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `refresh token returns same access token before expiration`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
            .andExpect(jsonPath("$.data.accessToken").value(session.accessToken))
            .andExpect(jsonPath("$.data.refreshToken").value(session.refreshToken))
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value(session.accessToken))
            .andExpect(jsonPath("$.data.refreshToken").value(session.refreshToken))
    }

    @Test
    fun `refresh token replaces expired access token once`() {
        val session = createLoginSession(password = "password", expired = true)

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("토큰이 재발급되었습니다."))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))

        mockMvc.perform(
            post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token":"${session.refreshToken}"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    @Test
    fun `delete account requires login session`() {
        mockMvc.perform(
            post("/auth/delete-account")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"password","leftData":true}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete account validates account credentials`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"wrong-password","leftData":true}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `delete account removes user and login sessions`() {
        val session = createLoginSession(password = "password")

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"password","leftData":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("회원 탈퇴가 완료되었습니다."))
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    private fun createLoginSession(password: String, expired: Boolean = false): TestLoginSession {
        val id = UUID.randomUUID()
        val email = "test-$id@example.com"
        val passwordHash = passwordEncoder.encode(password) ?: error("Password encoding failed.")
        val user = userAccountRepository.save(
            UserAccount(
                name = "test-user",
                email = email,
                passwordHash = passwordHash,
            ),
        )
        val issuedAt = if (expired) {
            Instant.now().minusSeconds(jwtProperties.accessTokenExpiresIn + 1)
        } else {
            Instant.now()
        }
        val accessToken = jwtProvider.generateAccessToken(user, issuedAt)
        val refreshToken = "refresh_$id"

        loginSessionRepository.save(
            LoginSession(
                sessionId = "sess_$id",
                accessToken = accessToken,
                userId = user.id,
                refreshToken = refreshToken,
                expiresAt = issuedAt.plusSeconds(jwtProperties.accessTokenExpiresIn),
            ),
        )

        return TestLoginSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = email,
            userId = user.id,
        )
    }

    private fun createGroup(name: String): Group =
        groupRepository.save(
            Group(
                name = name,
                inviteCode = "invite_${UUID.randomUUID().toString().take(8)}",
                createdAt = Instant.now(),
            ),
        )

    private data class TestLoginSession(
        val accessToken: String,
        val refreshToken: String,
        val email: String,
        val userId: Long,
    )
}
