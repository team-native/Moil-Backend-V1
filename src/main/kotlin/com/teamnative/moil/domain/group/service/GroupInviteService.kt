package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.CheckGroupInviteResponse
import com.teamnative.moil.domain.group.dto.JoinGroupResponse
import com.teamnative.moil.domain.group.dto.ProfileSelection
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@Service
class GroupInviteService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun check(user: UserAccount, inviteCode: String): CheckGroupInviteResponse {
        val group = findJoinableGroup(user, inviteCode)

        return CheckGroupInviteResponse(
            groupId = group.id,
            name = group.name,
            memberCount = groupMemberRepository.countByGroupId(group.id),
            inviteCode = group.inviteCode,
        )
    }

    @Transactional
    fun join(user: UserAccount, inviteCode: String, nickname: String, profile: ProfileSelection): JoinGroupResponse {
        val group = findJoinableGroup(user, inviteCode)

        try {
            groupMemberRepository.saveAndFlush(
                GroupMember(
                    groupId = group.id,
                    userId = user.id,
                    role = GroupRole.MEMBER,
                    notificationEnabled = true,
                    nickname = nickname,
                    color = profile.colorId,
                    imagePath = profile.imagePath,
                    joinedAt = Instant.now(clock),
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 참가한 그룹입니다.")
        }

        return JoinGroupResponse(
            groupId = group.id,
            name = group.name,
            myRole = GroupRole.MEMBER.toApiRole(),
            myNickname = nickname,
            myColor = profile.colorId,
            myImagePath = profile.imagePath,
        )
    }

    private fun findJoinableGroup(user: UserAccount, inviteCode: String): Group {
        val group = groupRepository.findByInviteCode(inviteCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "초대 코드를 찾을 수 없습니다.")

        if (groupMemberRepository.existsByGroupIdAndUserId(group.id, user.id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 참가한 그룹입니다.")
        }

        return group
    }
}
