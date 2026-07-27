package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.CheckGroupInviteResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class GroupInviteService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional(readOnly = true)
    fun check(user: UserAccount, inviteCode: String): CheckGroupInviteResponse {
        val group = groupRepository.findByInviteCode(inviteCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "초대 코드를 찾을 수 없습니다.")

        if (groupMemberRepository.existsByGroupIdAndUserId(group.id, user.id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "이미 참가한 그룹입니다.")
        }

        return CheckGroupInviteResponse(
            groupId = group.id,
            name = group.name,
            memberCount = groupMemberRepository.countByGroupId(group.id),
        )
    }
}
