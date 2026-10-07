package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.ProfileResponse
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.group.service.ProfileSelectionValidator
import com.teamnative.moil.domain.image.service.ImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProfileService(
    private val userAccountRepository: UserAccountRepository,
    private val profileSelectionValidator: ProfileSelectionValidator,
    private val imageService: ImageService,
) {

    @Transactional
    fun update(user: UserAccount, name: String, colorId: String? = null, imagePath: String? = null): ProfileResponse {
        val selection = if (colorId != null || imagePath != null) {
            profileSelectionValidator.requireProfileSelection(user, colorId, imagePath)
        } else {
            null
        }
        val materializedImagePath = selection?.imagePath?.let { imageService.materializeOwnedImagePath(user, it) }
        val updatedUser = userAccountRepository.save(
            user.copy(
                name = name.trim(),
                defaultColorId = if (selection != null) selection.colorId else user.defaultColorId,
                defaultImagePath = if (selection != null) materializedImagePath else user.defaultImagePath,
            ),
        )

        return updatedUser.toProfileResponse()
    }

    @Transactional(readOnly = true)
    fun get(user: UserAccount): ProfileResponse = user.toProfileResponse()

    private fun UserAccount.toProfileResponse() = ProfileResponse(
        userId = id,
        name = name,
        email = email,
        defaultColorId = defaultColorId,
        defaultImagePath = defaultImagePath,
    )
}
