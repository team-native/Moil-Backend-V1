package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.UpdateGroupNotificationResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupNotificationService(
    private val groupPermissionService: GroupPermissionService,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional
    fun update(user: UserAccount, groupId: Long, notificationEnabled: Boolean): UpdateGroupNotificationResponse {
        val access = groupPermissionService.requireMember(user, groupId)
        val member = groupMemberRepository.save(
            access.member.copy(notificationEnabled = notificationEnabled),
        )

        return UpdateGroupNotificationResponse(
            groupId = groupId,
            notificationEnabled = member.notificationEnabled,
        )
    }
}
