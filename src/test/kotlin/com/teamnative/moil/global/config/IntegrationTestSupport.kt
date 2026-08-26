package com.teamnative.moil.global.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.teamnative.moil.domain.auth.model.LoginSession
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.auth.repository.VerifiedSignupSessionRepository
import com.teamnative.moil.domain.auth.service.JwtProvider
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import com.teamnative.moil.domain.image.repository.PendingProfileImageRepository
import com.teamnative.moil.domain.image.repository.ProfileImageRepository
import com.teamnative.moil.domain.oauth.repository.SocialAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import java.time.Instant
import java.util.UUID

abstract class IntegrationTestSupport {

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    protected lateinit var loginSessionRepository: LoginSessionRepository

    @Autowired
    protected lateinit var verifiedSignupSessionRepository: VerifiedSignupSessionRepository

    @Autowired
    protected lateinit var groupRepository: GroupRepository

    @Autowired
    protected lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    protected lateinit var eventRepository: EventRepository

    @Autowired
    protected lateinit var profileImageRepository: ProfileImageRepository

    @Autowired
    protected lateinit var pendingProfileImageRepository: PendingProfileImageRepository

    @Autowired
    protected lateinit var socialAccountRepository: SocialAccountRepository

    @Autowired
    protected lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    protected lateinit var jwtProvider: JwtProvider

    @Autowired
    protected lateinit var jwtProperties: JwtProperties

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected fun createLoginSession(password: String, expired: Boolean = false): TestLoginSession {
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
                sessionId = id.toString(),
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

    protected fun createGroup(name: String): Group =
        groupRepository.save(
            Group(
                name = name,
                inviteCode = "invite_${UUID.randomUUID().toString().take(8)}",
                createdAt = Instant.now(),
            ),
        )

    protected fun createEvent(
        groupId: Long,
        userId: Long,
        title: String,
        startsAt: Instant,
        endsAt: Instant,
    ): Event =
        eventRepository.save(
            Event(
                groupId = groupId,
                creatorId = userId,
                updaterId = userId,
                title = title,
                startsAt = startsAt,
                endsAt = endsAt,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )

    protected data class TestLoginSession(
        val accessToken: String,
        val refreshToken: String,
        val email: String,
        val userId: Long,
    )
}
