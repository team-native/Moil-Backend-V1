package com.teamnative.moil.domain.image.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

/**
 * A single in-flight upload per user, held here until it is actually attached to a group
 * profile. Keeping this separate from [ProfileImage] means an upload the client abandons
 * (never used to create/join a group or update a profile) never accumulates storage: the
 * next upload for that user simply overwrites this row instead of adding a new one, and the
 * table can never hold more than one row per user.
 */
@Entity
@Table(name = "upload_queue")
data class PendingProfileImage(
    @Id
    val userId: Long,

    @Column(name = "pending_key", nullable = false, unique = true, length = 64)
    val key: String,

    @Lob
    @Column(nullable = false)
    val image: ByteArray,

    @Column(nullable = false, length = 100)
    val contentType: String,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PendingProfileImage) return false

        return userId == other.userId
    }

    override fun hashCode(): Int = userId.hashCode()
}
