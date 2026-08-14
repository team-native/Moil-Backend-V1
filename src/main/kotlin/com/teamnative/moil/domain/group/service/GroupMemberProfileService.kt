package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.ProfileSelection
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberProfileResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.image.service.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupMemberProfileService(
    private val groupPermissionService: GroupPermissionService,
    private val groupMemberRepository: GroupMemberRepository,
    private val imageService: ImageService,
) {

    @Transactional
    fun update(user: UserAccount, groupId: Long, nickname: String, profile: ProfileSelection): UpdateGroupMemberProfileResponse {
        val access = groupPermissionService.requireMember(user, groupId)
        val member = groupMemberRepository.save(
            access.member.copy(
                nickname = nickname.trim(),
                color = profile.colorId,
                imagePath = profile.imagePath,
            ),
        )

        if (profile.colorId != null) {
            groupMemberRepository.resetImageProfilesByUserId(user.id, profile.colorId)
            imageService.deleteByUser(user)
        }

        return UpdateGroupMemberProfileResponse(
            groupId = member.groupId,
            userId = member.userId,
            nickname = member.nickname,
            colorId = member.color,
            imagePath = member.imagePath,
        )
    }
}
