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
        val previousImagePath = access.member.imagePath
        val imagePath = profile.imagePath?.let { imageService.materializeOwnedImagePath(user, it) }

        val member = groupMemberRepository.save(
            access.member.copy(
                nickname = nickname.trim(),
                color = profile.colorId,
                imagePath = imagePath,
            ),
        )

        releasePreviousImageIfUnused(user, previousImagePath, imagePath)

        return UpdateGroupMemberProfileResponse(
            groupId = member.groupId,
            userId = member.userId,
            nickname = member.nickname,
            colorId = member.color,
            imagePath = member.imagePath,
        )
    }

    // Bounds storage to roughly one image per group a user customizes: once this group no
    // longer points at the old image, delete it - unless the user reused the same image in
    // another group, in which case it must stay alive for that group.
    private fun releasePreviousImageIfUnused(user: UserAccount, previousImagePath: String?, newImagePath: String?) {
        if (previousImagePath == null || previousImagePath == newImagePath) {
            return
        }

        if (!groupMemberRepository.existsByUserIdAndImagePath(user.id, previousImagePath)) {
            imageService.deleteByPath(user, previousImagePath)
        }
    }
}
