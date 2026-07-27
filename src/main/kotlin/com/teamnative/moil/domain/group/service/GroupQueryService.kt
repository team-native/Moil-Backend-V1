package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupQueryService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional(readOnly = true)
    fun findMyGroups(user: UserAccount): List<GroupSummaryResponse> {
        val members = groupMemberRepository.findAllByUserId(user.id)
        val groups = groupRepository.findAllById(members.map { it.groupId })
            .associateBy { it.id }

        return members.mapNotNull { member ->
            val group = groups[member.groupId] ?: return@mapNotNull null

            GroupSummaryResponse(
                groupId = group.id,
                name = group.name,
                role = member.role,
                memberCount = groupMemberRepository.countByGroupId(group.id),
                notificationEnabled = member.notificationEnabled,
            )
        }
    }
}
