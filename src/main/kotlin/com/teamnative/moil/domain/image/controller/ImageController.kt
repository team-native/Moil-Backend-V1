package com.teamnative.moil.domain.image.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.image.dto.ImageUploadResponse
import com.teamnative.moil.domain.image.service.ImageService
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/images")
class ImageController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val imageService: ImageService,
) {

    @PostMapping
    fun upload(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @RequestPart("image", required = false) image: MultipartFile?,
    ): ApiResponse<ImageUploadResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        val file = image
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 업로드해야 합니다.")

        return ApiResponse.success(
            message = "이미지가 업로드되었습니다.",
            data = imageService.upload(user, file),
        )
    }

    @GetMapping("/{key}")
    fun image(
        @PathVariable key: String,
    ): ResponseEntity<ByteArray> {
        val image = imageService.findByKey(key)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
            .body(image.image)
    }
}
