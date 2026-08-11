package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
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
    private val eventSharedMemberRepository: EventSharedMemberRepository,
) {

    @Transactional
    fun updateName(user: UserAccount, groupId: Long, name: String) {
        val access = groupPermissionService.requireManager(user, groupId)
        groupRepository.save(access.group.copy(name = name))
    }

    @Transactional
    fun updateMemberRole(
        user: UserAccount,
        groupId: Long,
        memberId: Long,
        role: GroupRole,
    ) {
        groupPermissionService.requireManager(user, groupId)

        if (role == GroupRole.OWNER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER role must be changed through transfer-admin.")
        }

        val targetMember = groupMemberRepository.findById(memberId).orElse(null)
            ?.takeIf { it.groupId == groupId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group member not found.")

        if (targetMember.role == GroupRole.OWNER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER role cannot be changed.")
        }

        groupMemberRepository.save(
            targetMember.copy(
                role = role,
                ownerGroupId = null,
            ),
        )
    }

    @Transactional
    fun updateMemberRoleByUserId(
        user: UserAccount,
        groupId: Long,
        targetUserId: Long,
        role: String,
    ) {
        val member = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group member not found.")

        updateMemberRole(user, groupId, member.id, role.toGroupRole())
    }

    @Transactional
    fun transferOwner(user: UserAccount, groupId: Long, memberId: Long) {
        val access = groupPermissionService.requireOwner(user, groupId)
        val targetMember = groupMemberRepository.findById(memberId).orElse(null)
            ?.takeIf { it.groupId == groupId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group member not found.")

        if (targetMember.id == access.member.id) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Current owner cannot be the transfer target.")
        }

        groupMemberRepository.saveAndFlush(
            access.member.copy(
                role = GroupRole.ADMIN,
                ownerGroupId = null,
            ),
        )
        groupMemberRepository.save(
            targetMember.copy(
                role = GroupRole.OWNER,
                ownerGroupId = groupId,
            ),
        )
    }

    @Transactional
    fun transferOwnerToUser(user: UserAccount, groupId: Long, targetUserId: Long) {
        val targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Group member not found.")

        transferOwner(user, groupId, targetMember.id)
    }

    @Transactional
    fun leave(user: UserAccount, groupId: Long) {
        val access = groupPermissionService.requireMember(user, groupId)

        if (access.member.role == GroupRole.OWNER) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "OWNER cannot leave a group.")
        }

        eventSharedMemberRepository.deleteByUserIdAndGroupId(user.id, groupId)
        groupMemberRepository.delete(access.member)
    }
}

private fun String.toGroupRole(): GroupRole =
    when (lowercase()) {
        "admin" -> GroupRole.ADMIN
        "member" -> GroupRole.MEMBER
        else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role.")
    }
