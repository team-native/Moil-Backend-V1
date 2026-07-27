package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberRoleResponse
import com.teamnative.moil.domain.group.dto.UpdateGroupNameResponse
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class GroupManagementService(
    private val groupPermissionService: GroupPermissionService,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    @Transactional
    fun updateName(user: UserAccount, groupId: Long, name: String): UpdateGroupNameResponse {
        val access = groupPermissionService.requireManager(user, groupId)
        val group = groupRepository.save(access.group.copy(name = name))

        return UpdateGroupNameResponse(
            groupId = group.id,
            name = group.name,
        )
    }

    @Transactional
    fun updateMemberRole(
        user: UserAccount,
        groupId: Long,
        memberId: Long,
        role: GroupRole,
    ): UpdateGroupMemberRoleResponse {
        groupPermissionService.requireManager(user, groupId)

        if (role == GroupRole.OWNER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER 권한은 양도 API를 사용해주세요.")
        }

        val targetMember = groupMemberRepository.findById(memberId).orElse(null)
            ?.takeIf { it.groupId == groupId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "대상 멤버를 찾을 수 없습니다.")

        if (targetMember.role == GroupRole.OWNER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER 권한은 변경할 수 없습니다.")
        }

        val updatedMember = groupMemberRepository.save(targetMember.copy(role = role))

        return UpdateGroupMemberRoleResponse(
            groupId = groupId,
            memberId = updatedMember.id,
            role = updatedMember.role,
        )
    }
}
