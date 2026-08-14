package com.teamnative.moil.domain.image.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.image.dto.ImageUploadResponse
import com.teamnative.moil.domain.image.model.ProfileImage
import com.teamnative.moil.domain.image.repository.ProfileImageRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ImageService(
    private val profileImageRepository: ProfileImageRepository,
    private val clock: Clock,
) {

    @Transactional
    fun upload(user: UserAccount, image: MultipartFile): ImageUploadResponse {
        val contentType = image.contentType?.trim().orEmpty()
        validateImage(image, contentType)

        val now = Instant.now(clock)
        val saved = profileImageRepository.save(
            profileImageRepository.findByUserId(user.id)
                ?.copy(
                    key = generateKey(),
                    image = image.bytes,
                    contentType = contentType,
                    updatedAt = now,
                )
                ?: ProfileImage(
                    key = generateKey(),
                    image = image.bytes,
                    userId = user.id,
                    contentType = contentType,
                    createdAt = now,
                    updatedAt = now,
                ),
        )

        return ImageUploadResponse(imagePath = imagePath(saved.key))
    }

    @Transactional(readOnly = true)
    fun findByKey(imageKey: String): ProfileImage =
        profileImageRepository.findByKey(imageKey)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.")

    private fun validateImage(image: MultipartFile, contentType: String) {
        if (image.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 업로드해야 합니다.")
        }

        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.")
        }

        if (image.size > MAX_IMAGE_SIZE_BYTES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 2MB 이하로 업로드해야 합니다.")
        }
    }

    private fun generateKey(): String {
        while (true) {
            val key = UUID.randomUUID().toString().replace("-", "")
            if (!profileImageRepository.existsByKey(key)) {
                return key
            }
        }
    }

    private fun imagePath(key: String): String = "/images/$key"

    companion object {
        private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private const val MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024
    }
}
