package com.teamnative.moil.domain.group.service

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
        val mismatches = groupMemberRepository.findOwnerGroupIdMismatches()

        mismatches
            .filter { it.role != GroupRole.OWNER }
            .forEach { member ->
                member.ownerGroupId = null
                groupMemberRepository.save(member)
            }
        groupMemberRepository.flush()

        mismatches
            .filter { it.role == GroupRole.OWNER }
            .forEach { member ->
                member.ownerGroupId = member.groupId
                groupMemberRepository.save(member)
            }
    }
}
