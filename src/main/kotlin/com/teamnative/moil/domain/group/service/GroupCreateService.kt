package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.CreateGroupResponse
import com.teamnative.moil.domain.group.dto.ProfileSelection
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import com.teamnative.moil.domain.image.service.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class GroupCreateService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val imageService: ImageService,
    private val clock: Clock,
) {

    @Transactional
    fun create(user: UserAccount, name: String, nickname: String, profile: ProfileSelection): CreateGroupResponse {
        val group = groupRepository.saveAndFlush(
            Group(
                name = name,
                inviteCode = generateInviteCode(),
                createdAt = Instant.now(clock),
            ),
        )
        val imagePath = profile.imagePath?.let { imageService.materializeOwnedImagePath(user, it) }

        groupMemberRepository.saveAndFlush(
            GroupMember(
                groupId = group.id,
                userId = user.id,
                ownerGroupId = group.id,
                role = GroupRole.OWNER,
                notificationEnabled = true,
                nickname = nickname,
                color = profile.colorId,
                imagePath = imagePath,
                joinedAt = Instant.now(clock),
            ),
        )

        return CreateGroupResponse(
            groupId = group.id,
            name = group.name,
            inviteCode = group.inviteCode,
            myRole = GroupRole.OWNER.toApiRole(),
        )
    }

    private fun generateInviteCode(): String {
        while (true) {
            val inviteCode = UUID.randomUUID().toString().replace("-", "").take(INVITE_CODE_LENGTH)
            if (!groupRepository.existsByInviteCode(inviteCode)) {
                return inviteCode
            }
        }
    }

    companion object {
        private const val INVITE_CODE_LENGTH = 12
    }
}

fun GroupRole.toApiRole(): String =
    when (this) {
        GroupRole.OWNER,
        GroupRole.ADMIN -> "admin"
        GroupRole.MEMBER -> "member"
    }
