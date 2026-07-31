package com.teamnative.moil.global.config

import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
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
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class GroupApiTest : IntegrationTestSupport() {

    @Test
    fun `my groups returns notion response keys`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Spec Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.OWNER,
                nickname = "Owner",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].groupId").value(group.id))
            .andExpect(jsonPath("$.data[0].name").value("Spec Group"))
            .andExpect(jsonPath("$.data[0].inviteCode").value(group.inviteCode))
            .andExpect(jsonPath("$.data[0].myRole").value("admin"))
            .andExpect(jsonPath("$.data[0].myNickname").value("Owner"))
            .andExpect(jsonPath("$.data[0].myColor").value("RED"))
            .andExpect(jsonPath("$.data[0].memberCount").value(1))
            .andExpect(jsonPath("$.data[0].role").doesNotExist())
            .andExpect(jsonPath("$.data[0].notificationEnabled").doesNotExist())
    }

    @Test
    fun `create group accepts profile keys and returns notion response keys`() {
        val session = createLoginSession(password = "password")
        val groupName = "Spec Group ${UUID.randomUUID()}"

        mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$groupName","nickname":"Moil","color":"BLUE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").exists())
            .andExpect(jsonPath("$.data.name").value(groupName))
            .andExpect(jsonPath("$.data.inviteCode").exists())
            .andExpect(jsonPath("$.data.myRole").value("admin"))
            .andExpect(jsonPath("$.data.role").doesNotExist())

        val group = groupRepository.findAll().first { it.name == groupName }
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Created owner member not found.")

        assertEquals(GroupRole.OWNER, member.role)
        assertEquals("Moil", member.nickname)
        assertEquals("BLUE", member.color)
    }

    @Test
    fun `join verify and join use notion paths and keys`() {
        val ownerSession = createLoginSession(password = "password")
        val joinSession = createLoginSession(password = "password")
        val group = createGroup(name = "Joinable Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = ownerSession.userId,
                role = GroupRole.OWNER,
                nickname = "Owner",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/join/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${joinSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("Joinable Group"))
            .andExpect(jsonPath("$.data.memberCount").value(1))
            .andExpect(jsonPath("$.data.inviteCode").value(group.inviteCode))

        mockMvc.perform(
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${joinSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}","nickname":"Guest","color":"GREEN"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("Joinable Group"))
            .andExpect(jsonPath("$.data.myRole").value("member"))
            .andExpect(jsonPath("$.data.myNickname").value("Guest"))
            .andExpect(jsonPath("$.data.myColor").value("GREEN"))
            .andExpect(jsonPath("$.data.memberCount").doesNotExist())

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, joinSession.userId)
            ?: error("Joined member not found.")

        assertEquals(GroupRole.MEMBER, member.role)
        assertEquals("Guest", member.nickname)
        assertEquals("GREEN", member.color)
    }

    @Test
    fun `group detail and members return notion response keys`() {
        val ownerSession = createLoginSession(password = "password")
        val memberSession = createLoginSession(password = "password")
        val group = createGroup(name = "Detail Group")
        groupMemberRepository.saveAll(
            listOf(
                GroupMember(
                    groupId = group.id,
                    userId = ownerSession.userId,
                    role = GroupRole.OWNER,
                    nickname = "Owner",
                    color = "RED",
                    joinedAt = Instant.now().minusSeconds(10),
                ),
                GroupMember(
                    groupId = group.id,
                    userId = memberSession.userId,
                    role = GroupRole.MEMBER,
                    nickname = "Member",
                    color = "GREEN",
                    joinedAt = Instant.now(),
                ),
            ),
        )

        mockMvc.perform(
            get("/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("Detail Group"))
            .andExpect(jsonPath("$.data.inviteCode").value(group.inviteCode))
            .andExpect(jsonPath("$.data.memberCount").value(2))
            .andExpect(jsonPath("$.data.monthlyEventCount").isNumber)
            .andExpect(jsonPath("$.data.myRole").value("admin"))
            .andExpect(jsonPath("$.data.members[0].userId").value(ownerSession.userId))
            .andExpect(jsonPath("$.data.members[0].nickname").value("Owner"))
            .andExpect(jsonPath("$.data.members[0].role").value("admin"))
            .andExpect(jsonPath("$.data.members[0].color").value("RED"))
            .andExpect(jsonPath("$.data.role").doesNotExist())
            .andExpect(jsonPath("$.data.notificationEnabled").doesNotExist())

        mockMvc.perform(
            get("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].userId").value(ownerSession.userId))
            .andExpect(jsonPath("$.data[0].nickname").value("Owner"))
            .andExpect(jsonPath("$.data[0].email").value(ownerSession.email))
            .andExpect(jsonPath("$.data[0].role").value("admin"))
            .andExpect(jsonPath("$.data[0].colorId").value("RED"))
            .andExpect(jsonPath("$.data[0].isMe").value(true))
            .andExpect(jsonPath("$.data[0].memberId").doesNotExist())
            .andExpect(jsonPath("$.data[0].joinedAt").doesNotExist())
    }

    @Test
    fun `notification update uses enabled request key`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Notification Group")
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
            patch("/groups/${group.id}/notification")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.notificationEnabled").value(false))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")

        assertEquals(false, member.notificationEnabled)
    }

    @Test
    fun `management endpoints return null data and mutate state`() {
        val ownerSession = createLoginSession(password = "password")
        val targetSession = createLoginSession(password = "password")
        val group = createGroup(name = "Management Group")
        groupMemberRepository.saveAll(
            listOf(
                GroupMember(
                    groupId = group.id,
                    userId = ownerSession.userId,
                    role = GroupRole.OWNER,
                    joinedAt = Instant.now().minusSeconds(10),
                ),
                GroupMember(
                    groupId = group.id,
                    userId = targetSession.userId,
                    role = GroupRole.MEMBER,
                    joinedAt = Instant.now(),
                ),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Renamed Group"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            patch("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"members":[{"userId":${targetSession.userId},"role":"admin"}]}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        mockMvc.perform(
            post("/groups/${group.id}/transfer-admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"targetUserId":${targetSession.userId}}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        assertEquals("Renamed Group", groupRepository.findById(group.id).orElseThrow().name)
        assertEquals(
            GroupRole.ADMIN,
            groupMemberRepository.findByGroupIdAndUserId(group.id, ownerSession.userId)?.role,
        )
        assertEquals(
            GroupRole.OWNER,
            groupMemberRepository.findByGroupIdAndUserId(group.id, targetSession.userId)?.role,
        )
    }

    @Test
    fun `leave group uses notion path and returns null data`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Leave Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            delete("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        assertNull(groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId))
    }
}
