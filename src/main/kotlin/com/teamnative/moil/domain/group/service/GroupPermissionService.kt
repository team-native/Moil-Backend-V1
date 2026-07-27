package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class GroupPermissionService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    fun requireMember(user: UserAccount, groupId: Long): GroupAccess {
        val group = groupRepository.findById(groupId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다.")
        val member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.id)
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "그룹 멤버가 아닙니다.")

        return GroupAccess(group, member)
    }

    fun requireAnyRole(user: UserAccount, groupId: Long, roles: Set<GroupRole>): GroupAccess {
        val access = requireMember(user, groupId)

        if (access.member.role !in roles) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "그룹 권한이 없습니다.")
        }

        return access
    }

    fun requireOwner(user: UserAccount, groupId: Long): GroupAccess =
        requireAnyRole(user, groupId, setOf(GroupRole.OWNER))

    fun requireManager(user: UserAccount, groupId: Long): GroupAccess =
        requireAnyRole(user, groupId, setOf(GroupRole.OWNER, GroupRole.ADMIN))

    data class GroupAccess(
        val group: Group,
        val member: GroupMember,
    )
}
