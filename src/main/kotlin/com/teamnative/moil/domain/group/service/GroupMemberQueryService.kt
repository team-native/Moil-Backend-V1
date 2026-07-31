package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.group.dto.GroupMemberResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMemberQueryService(
    private val groupPermissionService: GroupPermissionService,
    private val groupMemberRepository: GroupMemberRepository,
    private val userAccountRepository: UserAccountRepository,
) {

    @Transactional(readOnly = true)
    fun findMembers(user: UserAccount, groupId: Long): List<GroupMemberResponse> {
        groupPermissionService.requireMember(user, groupId)

        val members = groupMemberRepository.findAllByGroupId(groupId)
            .sortedBy { it.joinedAt }
        val users = userAccountRepository.findAllById(members.map { it.userId })
            .associateBy { it.id }

        return members.mapNotNull { member ->
            val memberUser = users[member.userId] ?: return@mapNotNull null

            GroupMemberResponse(
                userId = member.userId,
                nickname = member.nickname,
                email = memberUser.email,
                role = member.role.toApiRole(),
                colorId = member.color,
                isMe = member.userId == user.id,
            )
        }
    }
}
