package com.teamnative.moil.domain.image.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.image.dto.ImageUploadResponse
import com.teamnative.moil.domain.image.model.PendingProfileImage
import com.teamnative.moil.domain.image.model.ProfileImage
import com.teamnative.moil.domain.image.repository.PendingProfileImageRepository
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
    private val pendingProfileImageRepository: PendingProfileImageRepository,
    private val clock: Clock,
) {

    /**
     * Uploads always land in the per-user upload queue, not permanent storage. A second upload
     * before the first is ever attached to a group simply overwrites the queued file - so an
     * abandoned upload never grows storage beyond one pending image per user.
     */
    @Transactional
    fun upload(user: UserAccount, image: MultipartFile): ImageUploadResponse {
        if (image.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 업로드해야 합니다.")
        }

        if (image.size > MAX_IMAGE_SIZE_BYTES) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 2MB 이하로 업로드해야 합니다.")
        }

        val bytes = image.bytes
        val contentType = detectContentType(bytes)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.")

        val now = Instant.now(clock)
        val saved = pendingProfileImageRepository.save(
            PendingProfileImage(
                userId = user.id,
                key = generateKey(),
                image = bytes,
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
            ?: pendingProfileImageRepository.findByKey(imageKey)?.toProfileImage()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.")

    /**
     * Verifies the caller owns the referenced image and, if it is still sitting in the upload
     * queue, materializes it into permanent storage now - this is the moment an image actually
     * becomes "in use" by a group profile, and the reason it survives from here on. An
     * already-materialized image (e.g. reused for a second group) is left exactly as-is.
     */
    @Transactional
    fun requireOwnedImagePath(user: UserAccount, imagePath: String): String {
        val key = parseKey(imagePath)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 경로 형식이 올바르지 않습니다.")

        profileImageRepository.findByKey(key)?.let { image ->
            if (image.userId != user.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 이미지만 사용할 수 있습니다.")
            }
            return imagePath(key)
        }

        val pending = pendingProfileImageRepository.findByKey(key)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "이미지를 찾을 수 없습니다.")

        if (pending.userId != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 업로드한 이미지만 사용할 수 있습니다.")
        }

        profileImageRepository.save(pending.toProfileImage())
        pendingProfileImageRepository.deleteByUserId(pending.userId)

        return imagePath(key)
    }

    @Transactional
    fun deleteByUser(user: UserAccount) {
        profileImageRepository.deleteByUserId(user.id)
        pendingProfileImageRepository.deleteByUserId(user.id)
    }

    /**
     * Deletes a single materialized image owned by the user, identified by its public path.
     * Used to release storage once no group profile references it anymore (e.g. replaced by a
     * new image, switched to a color, or the group was left).
     */
    @Transactional
    fun deleteByPath(user: UserAccount, imagePath: String) {
        val key = parseKey(imagePath) ?: return

        profileImageRepository.findByKey(key)
            ?.takeIf { it.userId == user.id }
            ?.let { profileImageRepository.delete(it) }
    }

    private fun parseKey(imagePath: String): String? =
        imagePath.removePrefix(IMAGE_PATH_PREFIX)
            .takeIf { imagePath.startsWith(IMAGE_PATH_PREFIX) && it.isNotBlank() && '/' !in it }

    private fun generateKey(): String {
        while (true) {
            val key = UUID.randomUUID().toString().replace("-", "")
            if (!profileImageRepository.existsByKey(key) && !pendingProfileImageRepository.existsByKey(key)) {
                return key
            }
        }
    }

    private fun imagePath(key: String): String = "$IMAGE_PATH_PREFIX$key"

    private fun PendingProfileImage.toProfileImage(): ProfileImage = ProfileImage(
        key = key,
        image = image,
        userId = userId,
        contentType = contentType,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /**
     * Determines the real image format from the file's signature (magic bytes) rather than
     * trusting the client-supplied Content-Type header. Without this, a caller could label
     * arbitrary binary content as "image/png" and have it stored and served back - publicly,
     * unauthenticated, and cached for a year - from GET /images/{key}.
     */
    private fun detectContentType(bytes: ByteArray): String? = when {
        bytes.startsWith(PNG_SIGNATURE) -> "image/png"
        bytes.startsWith(JPEG_SIGNATURE) -> "image/jpeg"
        bytes.size >= WEBP_HEADER_SIZE &&
            bytes.startsWith(RIFF_SIGNATURE) &&
            bytes.copyOfRange(WEBP_FORMAT_OFFSET, WEBP_HEADER_SIZE).contentEquals(WEBP_SIGNATURE) -> "image/webp"
        else -> null
    }

    private fun ByteArray.startsWith(signature: ByteArray): Boolean =
        size >= signature.size && signature.indices.all { this[it] == signature[it] }

    companion object {
        private const val IMAGE_PATH_PREFIX = "/images/"
        private const val MAX_IMAGE_SIZE_BYTES = 2 * 1024 * 1024
        private const val WEBP_FORMAT_OFFSET = 8
        private const val WEBP_HEADER_SIZE = 12

        private val PNG_SIGNATURE =
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val JPEG_SIGNATURE =
            byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val RIFF_SIGNATURE =
            byteArrayOf('R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())
        private val WEBP_SIGNATURE =
            byteArrayOf('W'.code.toByte(), 'E'.code.toByte(), 'B'.code.toByte(), 'P'.code.toByte())
    }
}
