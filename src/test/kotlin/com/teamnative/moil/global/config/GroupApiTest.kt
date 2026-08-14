package com.teamnative.moil.global.config

import com.teamnative.moil.domain.event.model.EventSharedMember
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class GroupApiTest : IntegrationTestSupport() {

    @Autowired
    private lateinit var eventSharedMemberRepository: EventSharedMemberRepository

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
                .content("""{"name":"$groupName","nickname":"Moil","colorId":"BLUE"}"""),
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
    fun `create group accepts image profile`() {
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val groupName = "Image Group ${UUID.randomUUID()}"

        mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$groupName","nickname":"Moil","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").exists())

        val group = groupRepository.findAll().first { it.name == groupName }
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Created owner member not found.")

        assertNull(member.color)
        assertEquals(imagePath, member.imagePath)
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
                .content("""{"inviteCode":"${group.inviteCode}","nickname":"Guest","colorId":"GREEN"}"""),
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
    fun `join group accepts image profile and returns image path`() {
        val ownerSession = createLoginSession(password = "password")
        val joinSession = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(joinSession.accessToken)
        val group = createGroup(name = "Image Join Group")
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
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${joinSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}","nickname":"Guest","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.myColor").doesNotExist())
            .andExpect(jsonPath("$.data.myImagePath").value(imagePath))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, joinSession.userId)
            ?: error("Joined member not found.")

        assertNull(member.color)
        assertEquals(imagePath, member.imagePath)
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
    fun `group detail allows logged in non member before joining`() {
        val ownerSession = createLoginSession(password = "password")
        val nonMemberSession = createLoginSession(password = "password")
        val group = createGroup(name = "Invite Preview Group")
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
            get("/groups/${group.id}")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${nonMemberSession.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.name").value("Invite Preview Group"))
            .andExpect(jsonPath("$.data.inviteCode").value(group.inviteCode))
            .andExpect(jsonPath("$.data.memberCount").value(1))
            .andExpect(jsonPath("$.data.myRole").doesNotExist())
            .andExpect(jsonPath("$.data.members[0].userId").value(ownerSession.userId))
            .andExpect(jsonPath("$.data.members[0].nickname").value("Owner"))
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
    fun `group member profile update changes my nickname and color`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Profile Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Before",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","colorId":"BLUE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.status").value(0))
            .andExpect(jsonPath("$.message").value("프로필이 변경되었습니다."))
            .andExpect(jsonPath("$.data.groupId").value(group.id))
            .andExpect(jsonPath("$.data.userId").value(session.userId))
            .andExpect(jsonPath("$.data.nickname").value("After"))
            .andExpect(jsonPath("$.data.colorId").value("BLUE"))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")

        assertEquals("After", member.nickname)
        assertEquals("BLUE", member.color)
    }

    @Test
    fun `group member profile update changes to image and members return image path`() {
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "Image Profile Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Before",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname").value("After"))
            .andExpect(jsonPath("$.data.colorId").doesNotExist())
            .andExpect(jsonPath("$.data.imagePath").value(imagePath))

        mockMvc.perform(
            get("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].colorId").doesNotExist())
            .andExpect(jsonPath("$.data[0].imagePath").value(imagePath))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")

        assertEquals("After", member.nickname)
        assertNull(member.color)
        assertEquals(imagePath, member.imagePath)
    }

    @Test
    fun `group member profile update rejects invalid profile selection`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "Invalid Profile Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Before",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After"}"""),
        )
            .andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","colorId":"BLUE","imagePath":"/images/abc"}"""),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `group member profile update rejects other user image`() {
        val ownerSession = createLoginSession(password = "password")
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(ownerSession.accessToken)
        val group = createGroup(name = "Forbidden Image Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Before",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `group member profile update to color deletes uploaded image`() {
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "Delete Image Profile Group")
        val otherGroup = createGroup(name = "Other Image Profile Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Before",
                color = null,
                imagePath = imagePath,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = otherGroup.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Other",
                color = null,
                imagePath = imagePath,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","colorId":"BLUE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.colorId").value("BLUE"))
            .andExpect(jsonPath("$.data.imagePath").doesNotExist())

        assertNull(profileImageRepository.findByUserId(session.userId))
        val otherMember = groupMemberRepository.findByGroupIdAndUserId(otherGroup.id, session.userId)
            ?: error("Other group member not found.")
        assertEquals("BLUE", otherMember.color)
        assertNull(otherMember.imagePath)
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
        val otherGroup = createGroup(name = "Other Group")
        val groupEvent = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "Leave Target Event",
            startsAt = Instant.now().plusSeconds(3600),
            endsAt = Instant.now().plusSeconds(7200),
        )
        val otherGroupEvent = createEvent(
            groupId = otherGroup.id,
            userId = session.userId,
            title = "Other Group Event",
            startsAt = Instant.now().plusSeconds(3600),
            endsAt = Instant.now().plusSeconds(7200),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        eventSharedMemberRepository.saveAll(
            listOf(
                EventSharedMember(
                    eventId = groupEvent.id,
                    userId = session.userId,
                    createdAt = Instant.now(),
                ),
                EventSharedMember(
                    eventId = otherGroupEvent.id,
                    userId = session.userId,
                    createdAt = Instant.now(),
                ),
            ),
        )

        mockMvc.perform(
            delete("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").doesNotExist())

        assertNull(groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId))
        assertEquals(emptyList<EventSharedMember>(), eventSharedMemberRepository.findAllByEventId(groupEvent.id))
        assertEquals(1, eventSharedMemberRepository.findAllByEventId(otherGroupEvent.id).size)
    }

    private fun uploadProfileImage(accessToken: String): String {
        val file = MockMultipartFile("image", "profile.png", "image/png", byteArrayOf(1, 2, 3))
        val result = mockMvc.perform(
            multipart("/images")
                .file(file)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken"),
        )
            .andExpect(status().isOk)
            .andReturn()

        return objectMapper.readTree(result.response.contentAsString)
            .path("data")
            .path("imagePath")
            .asText()
    }
}
