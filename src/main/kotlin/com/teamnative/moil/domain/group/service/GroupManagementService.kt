package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.UpdateGroupNameResponse
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupManagementService(
    private val groupPermissionService: GroupPermissionService,
    private val groupRepository: GroupRepository,
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
}
