package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMemberIntegrityService(
    private val groupMemberRepository: GroupMemberRepository,
) {

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun syncOwnerGroupIds() {
        groupMemberRepository.clearOwnerGroupIds()
        groupMemberRepository.flush()

        val members = groupMemberRepository.findAll()

        members
            .groupBy { it.groupId }
            .values
            .forEach { syncGroupOwners(it) }
    }

    private fun syncGroupOwners(members: List<GroupMember>) {
        val owners = members
            .filter { it.role == GroupRole.OWNER }
            .sortedBy { it.id }

        owners.drop(1)
            .forEach { owner ->
                groupMemberRepository.save(owner.copy(role = GroupRole.ADMIN, ownerGroupId = null))
            }

        owners.firstOrNull()
            ?.let { owner ->
                groupMemberRepository.save(owner.copy(ownerGroupId = owner.groupId))
            }
    }
}
