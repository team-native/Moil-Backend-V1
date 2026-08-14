package com.teamnative.moil.domain.image.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.image.dto.ImageUploadResponse
import com.teamnative.moil.domain.image.service.ImageService
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/images")
class ImageController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val imageService: ImageService,
) {

    @PostMapping
    fun upload(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestPart("image") image: MultipartFile,
    ): ApiResponse<ImageUploadResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "이미지가 업로드되었습니다.",
            data = imageService.upload(user, image),
        )
    }
}
