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
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 색상 또는 이미지를 선택해야 합니다.")
        }

        if (color != null && image != null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 색상과 이미지는 동시에 선택할 수 없습니다.")
        }

        if (color != null && !MoilColor.exists(color)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 프로필 색상입니다.")
        }

        return ProfileSelection(
            colorId = color,
            imagePath = image?.let { imageService.requireOwnedImagePath(user, it) },
        )
    }
}
