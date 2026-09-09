package com.teamnative.moil.global.config

import com.teamnative.moil.domain.event.model.EventSharedMember
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.global.model.MoilColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `created group can immediately return owner member and empty monthly events`() {
        val session = createLoginSession(password = "password")
        val groupName = "Fresh Group ${UUID.randomUUID()}"

        val createResult = mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$groupName","nickname":"Owner","colorId":"GREEN"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.inviteCode").exists())
            .andReturn()

        val groupId = objectMapper
            .readTree(createResult.response.contentAsString)
            .path("data")
            .path("groupId")
            .asLong()

        mockMvc.perform(
            get("/groups/$groupId/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].userId").value(session.userId))
            .andExpect(jsonPath("$.data[0].nickname").value("Owner"))
            .andExpect(jsonPath("$.data[0].role").value("admin"))
            .andExpect(jsonPath("$.data[0].colorId").value("GREEN"))
            .andExpect(jsonPath("$.data[0].isMe").value(true))
            .andExpect(jsonPath("$.data[1]").doesNotExist())

        mockMvc.perform(
            get("/groups/$groupId/events")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .param("month", "2026-08"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    fun `profile color palette includes rainbow and spring colors`() {
        val expectedColorIds = listOf(
            "RED",
            "ORANGE",
            "YELLOW",
            "GREEN",
            "BLUE",
            "NAVY",
            "PURPLE",
            "PINK",
            "CREAM",
            "PEACH",
            "APRICOT",
            "TAN",
            "GOLD",
            "CORAL",
            "ROSE",
            "SKY",
            "LIGHT_BLUE",
            "MINT",
            "TEAL",
            "LIGHT_GREEN",
            "VIVID_GREEN",
            "VIOLET",
            "MAGENTA",
            "VIVID_ORANGE",
            "VIVID_RED",
            "WARM_PINK",
        )

        expectedColorIds.forEach { colorId ->
            assertTrue(MoilColor.exists(colorId), "$colorId must be a supported colorId")
        }
    }

    @Test
    fun `create group accepts added profile color ids`() {
        val session = createLoginSession(password = "password")
        val groupName = "Palette Group ${UUID.randomUUID()}"

        mockMvc.perform(
            post("/groups")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"$groupName","nickname":"Moil","colorId":"VIVID_ORANGE"}"""),
        )
            .andExpect(status().isOk)

        val group = groupRepository.findAll().first { it.name == groupName }
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Created owner member not found.")

        assertEquals("VIVID_ORANGE", member.color)
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

        assertEquals("RED", member.color)
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
            .andExpect(jsonPath("$.data.myColor").value("RED"))
            .andExpect(jsonPath("$.data.myImagePath").value(imagePath))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, joinSession.userId)
            ?: error("Joined member not found.")

        assertEquals("RED", member.color)
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
            .andExpect(jsonPath("$.data.colorId").value("RED"))
            .andExpect(jsonPath("$.data.imagePath").value(imagePath))

        mockMvc.perform(
            get("/groups/${group.id}/members")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].colorId").value("RED"))
            .andExpect(jsonPath("$.data[0].imagePath").value(imagePath))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")

        assertEquals("After", member.nickname)
        assertEquals("RED", member.color)
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

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","colorId":"UNKNOWN_COLOR"}"""),
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
    fun `failed profile update does not materialize pending image`() {
        val ownerSession = createLoginSession(password = "password")
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "No Materialize On Failure Group")
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
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isForbidden)

        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)
        assertEquals(true, pendingProfileImageRepository.findById(session.userId).isPresent)
    }

    @Test
    fun `failed group join does not materialize pending image`() {
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "No Materialize On Join Failure Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/groups/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"inviteCode":"${group.inviteCode}","nickname":"Again","imagePath":"$imagePath"}"""),
        )
            .andExpect(status().isConflict)

        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)
        assertEquals(true, pendingProfileImageRepository.findById(session.userId).isPresent)
    }

    @Test
    fun `group member profile update to color does not affect image in other groups`() {
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
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = otherGroup.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Other",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )
        // Attach the same image to both groups through the real update flow, so it is actually
        // materialized into permanent storage - just seeding GroupMember.imagePath directly would
        // leave the upload sitting in the queue and never exercise the release logic below.
        attachImageToGroup(session.accessToken, group.id, "Before", imagePath)
        attachImageToGroup(session.accessToken, otherGroup.id, "Other", imagePath)

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","colorId":"BLUE"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.colorId").value("BLUE"))
            .andExpect(jsonPath("$.data.imagePath").doesNotExist())

        val updatedMember = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")
        assertEquals("BLUE", updatedMember.color)
        assertNull(updatedMember.imagePath)

        // Switching this group to a color must not touch the image still in use by other groups.
        val otherMember = groupMemberRepository.findByGroupIdAndUserId(otherGroup.id, session.userId)
            ?: error("Other group member not found.")
        assertNull(otherMember.color)
        assertEquals(imagePath, otherMember.imagePath)
        assertEquals(1, profileImageRepository.findAllByUserId(session.userId).size)
    }

    @Test
    fun `replacing a group image releases the previous one when unused elsewhere`() {
        val session = createLoginSession(password = "password")
        val firstImagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "Replace Image Group")
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
        // Materialize the first image via the real attach flow so this test genuinely exercises
        // release-on-replace, instead of vacuously passing because the image was never stored.
        attachImageToGroup(session.accessToken, group.id, "Before", firstImagePath)

        val secondImagePath = uploadProfileImage(session.accessToken)

        mockMvc.perform(
            patch("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"After","imagePath":"$secondImagePath"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.imagePath").value(secondImagePath))

        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)
            ?: error("Group member not found.")
        assertEquals(secondImagePath, member.imagePath)

        // The old image is no longer referenced anywhere, so it should be released - this
        // keeps storage bounded to roughly one image per group instead of growing on every
        // re-upload.
        val remaining = profileImageRepository.findAllByUserId(session.userId)
        assertEquals(1, remaining.size)
        assertEquals(secondImagePath, "/images/" + remaining.single().key)
    }

    @Test
    fun `leaving a group releases its image when unused elsewhere`() {
        val session = createLoginSession(password = "password")
        val imagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "Leave Image Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )
        attachImageToGroup(session.accessToken, group.id, "Member", imagePath)
        assertEquals(1, profileImageRepository.findAllByUserId(session.userId).size)

        mockMvc.perform(
            delete("/groups/${group.id}/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isOk)

        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)
    }

    @Test
    fun `same user can use different images in different groups`() {
        val session = createLoginSession(password = "password")
        val firstImagePath = uploadProfileImage(session.accessToken)
        val group = createGroup(name = "First Image Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )
        // Materialize (and thereby clear from the queue) before the second upload, since the
        // upload queue only ever holds one pending image per user.
        attachImageToGroup(session.accessToken, group.id, "Member", firstImagePath)

        val secondImagePath = uploadProfileImage(session.accessToken)
        val otherGroup = createGroup(name = "Second Image Group")
        groupMemberRepository.save(
            GroupMember(
                groupId = otherGroup.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                nickname = "Member",
                color = "RED",
                joinedAt = Instant.now(),
            ),
        )
        attachImageToGroup(session.accessToken, otherGroup.id, "Member", secondImagePath)

        assertEquals(
            firstImagePath,
            groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId)?.imagePath,
        )
        assertEquals(
            secondImagePath,
            groupMemberRepository.findByGroupIdAndUserId(otherGroup.id, session.userId)?.imagePath,
        )
        assertEquals(2, profileImageRepository.findAllByUserId(session.userId).size)
    }

    @Test
    fun `uploading without ever attaching does not create a permanent image`() {
        val session = createLoginSession(password = "password")
        uploadProfileImage(session.accessToken)

        // Never used to create, join, or update a group profile - so it must still be sitting
        // in the upload queue rather than having leaked into permanent storage.
        assertEquals(0, profileImageRepository.findAllByUserId(session.userId).size)
        assertEquals(true, pendingProfileImageRepository.findById(session.userId).isPresent)
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

    // Attaches an image to a group profile through the real update endpoint, which is what
    // actually materializes a queued upload into permanent storage. Seeding GroupMember.imagePath
    // directly via the repository would skip that materialization step entirely.
    private fun attachImageToGroup(accessToken: String, groupId: Long, nickname: String, imagePath: String) {
        mockMvc.perform(
            patch("/groups/$groupId/members/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"nickname":"$nickname","imagePath":"$imagePath"}"""),
        ).andExpect(status().isOk)
    }

    private fun uploadProfileImage(accessToken: String): String {
        val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val file = MockMultipartFile("image", "profile.png", "image/png", pngSignature + byteArrayOf(1, 2, 3))
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
