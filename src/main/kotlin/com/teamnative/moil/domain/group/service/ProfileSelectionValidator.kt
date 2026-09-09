package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.group.dto.ProfileSelection
import com.teamnative.moil.domain.image.service.ImageService
import com.teamnative.moil.global.model.MoilColor
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class ProfileSelectionValidator(
    private val imageService: ImageService,
) {

    fun requireProfileSelection(user: UserAccount, colorId: String?, imagePath: String?): ProfileSelection {
        val color = colorId?.trim()?.takeIf { it.isNotBlank() }
        val image = imagePath?.trim()?.takeIf { it.isNotBlank() }

        if (color == null && image == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A profile color or image is required")
        }
        if (color != null && !MoilColor.exists(color)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported profile color")
        }

        val ownedImagePath = image?.let { imageService.requireOwnedImagePath(user, it) }
        return ProfileSelection(
            // An explicit color wins. Otherwise the image's dominant RGB cluster supplies it.
            colorId = color ?: ownedImagePath?.let { imageService.colorForOwnedImagePath(user, it) },
            imagePath = ownedImagePath,
        )
    }
}
