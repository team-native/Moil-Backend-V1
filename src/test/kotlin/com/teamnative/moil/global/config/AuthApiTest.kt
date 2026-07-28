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
class AuthApiTest : IntegrationTestSupport() {

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
                .content("""{"verifyId":"${UUID.randomUUID()}","code":"123456"}"""),
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
                .content("""{"sessionId":"${UUID.randomUUID()}","password":"password1","pwd":"password2"}"""),
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
                .content("""{"sessionId":"${UUID.randomUUID()}","password":"password","pwd":"password"}"""),
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
                .content("""{"sessionId":"${UUID.randomUUID()}","password":"password1","pwd":"password2"}"""),
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
                .content("""{"sessionId":"${UUID.randomUUID()}","password":"password","pwd":"password"}"""),
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

    @Test
    fun `delete account anonymizes user and keeps calendar data when left data is true`() {
        val session = createLoginSession(password = "password")
        val group = createGroup(name = "kept-group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = session.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )
        val event = createEvent(
            groupId = group.id,
            userId = session.userId,
            title = "kept-event",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"password","leftData":true}"""),
        )
            .andExpect(status().isOk)

        val anonymizedUser = userAccountRepository.findById(session.userId).orElseThrow()

        assert(anonymizedUser.name.startsWith("user_"))
        assert(anonymizedUser.email.startsWith("deleted_"))
        assert(anonymizedUser.email.endsWith("@deleted.local"))
        assert(anonymizedUser.email != session.email)
        assert(!eventRepository.existsById(event.id))
        assert(!groupRepository.existsById(group.id))
        assert(groupMemberRepository.findByGroupIdAndUserId(group.id, session.userId) == null)

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"password"}"""),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 일치하지 않습니다."))

        val verifiedSession = verifiedSignupSessionRepository.save(
            VerifiedSignupSession(
                sessionId = UUID.randomUUID().toString(),
                email = session.email,
                expiresAt = Instant.now().plusSeconds(300),
            ),
        )

        mockMvc.perform(
            post("/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sessionId":"${verifiedSession.sessionId}",
                      "password":"new-password",
                      "pwd":"new-password"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value(session.email))

        mockMvc.perform(
            post("/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("로그인되어 있지 않습니다."))
    }

    @Test
    fun `delete account transfers owned group to highest ranked member when left data is true`() {
        val ownerSession = createLoginSession(password = "password")
        val memberSession = createLoginSession(password = "password")
        val firstAdminSession = createLoginSession(password = "password")
        val secondAdminSession = createLoginSession(password = "password")
        val group = createGroup(name = "transfer-group")
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = ownerSession.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = memberSession.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = secondAdminSession.userId,
                role = GroupRole.ADMIN,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = group.id,
                userId = firstAdminSession.userId,
                role = GroupRole.ADMIN,
                joinedAt = Instant.now(),
            ),
        )

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${ownerSession.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${ownerSession.email}","password":"password","leftData":true}"""),
        )
            .andExpect(status().isOk)

        val previousOwner = groupMemberRepository.findByGroupIdAndUserId(group.id, ownerSession.userId)
            ?: error("Previous owner member not found.")
        val nextOwner = groupMemberRepository.findByGroupIdAndUserId(group.id, firstAdminSession.userId)
            ?: error("Next owner member not found.")
        val secondAdmin = groupMemberRepository.findByGroupIdAndUserId(group.id, secondAdminSession.userId)
            ?: error("Second admin member not found.")
        val member = groupMemberRepository.findByGroupIdAndUserId(group.id, memberSession.userId)
            ?: error("Member not found.")

        assert(previousOwner.role == GroupRole.MEMBER)
        assert(previousOwner.ownerGroupId == null)
        assert(nextOwner.role == GroupRole.OWNER)
        assert(nextOwner.ownerGroupId == group.id)
        assert(secondAdmin.role == GroupRole.ADMIN)
        assert(member.role == GroupRole.MEMBER)
    }

    @Test
    fun `delete account removes own events and owned groups when left data is false`() {
        val session = createLoginSession(password = "password")
        val otherSession = createLoginSession(password = "password")
        val ownedGroup = createGroup(name = "owner-group")
        val joinedGroup = createGroup(name = "joined-group")
        groupMemberRepository.save(
            GroupMember(
                groupId = ownedGroup.id,
                userId = session.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = ownedGroup.id,
                userId = otherSession.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = joinedGroup.id,
                userId = otherSession.userId,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )
        groupMemberRepository.save(
            GroupMember(
                groupId = joinedGroup.id,
                userId = session.userId,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )
        val ownEvent = createEvent(
            groupId = joinedGroup.id,
            userId = session.userId,
            title = "own-event",
            startsAt = Instant.parse("2026-01-10T10:00:00Z"),
            endsAt = Instant.parse("2026-01-10T11:00:00Z"),
        )
        val ownedGroupEvent = createEvent(
            groupId = ownedGroup.id,
            userId = otherSession.userId,
            title = "owned-group-event",
            startsAt = Instant.parse("2026-01-11T10:00:00Z"),
            endsAt = Instant.parse("2026-01-11T11:00:00Z"),
        )
        val remainingEvent = createEvent(
            groupId = joinedGroup.id,
            userId = otherSession.userId,
            title = "remaining-event",
            startsAt = Instant.parse("2026-01-12T10:00:00Z"),
            endsAt = Instant.parse("2026-01-12T11:00:00Z"),
        )

        mockMvc.perform(
            post("/auth/delete-account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${session.accessToken}")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"${session.email}","password":"password","leftData":false}"""),
        )
            .andExpect(status().isOk)

        assert(!eventRepository.existsById(ownEvent.id))
        assert(!eventRepository.existsById(ownedGroupEvent.id))
        assert(eventRepository.existsById(remainingEvent.id))
        assert(!groupRepository.existsById(ownedGroup.id))
        assert(groupRepository.existsById(joinedGroup.id))
        assert(groupMemberRepository.findAllByGroupId(ownedGroup.id).isEmpty())
        assert(groupMemberRepository.findAllByUserId(session.userId).isEmpty())
    }
}
