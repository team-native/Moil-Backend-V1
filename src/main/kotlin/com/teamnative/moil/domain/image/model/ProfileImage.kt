package com.teamnative.moil.domain.image.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "images")
data class ProfileImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "image_key", nullable = false, unique = true, length = 64)
    val key: String,

    @Lob
    @Column(nullable = false)
    val image: ByteArray,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 100)
    val contentType: String,

    @Column(nullable = false)
    val createdAt: Instant,

    @Column(nullable = false)
    val updatedAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileImage) return false

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
