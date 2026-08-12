package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberProfileResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMemberProfileService(
    private val groupPermissionService: GroupPermissionService,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional
    fun update(user: UserAccount, groupId: Long, nickname: String, colorId: String): UpdateGroupMemberProfileResponse {
        val access = groupPermissionService.requireMember(user, groupId)
        val member = groupMemberRepository.save(
            access.member.copy(
                nickname = nickname.trim(),
                color = colorId.trim(),
            ),
        )

        return UpdateGroupMemberProfileResponse(
            groupId = member.groupId,
            userId = member.userId,
            nickname = member.nickname,
            colorId = member.color,
        )
    }
}
