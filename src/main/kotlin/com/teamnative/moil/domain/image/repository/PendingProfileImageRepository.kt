package com.teamnative.moil.domain.image.repository

import com.teamnative.moil.domain.image.model.PendingProfileImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PendingProfileImageRepository : JpaRepository<PendingProfileImage, Long> {
    fun findByKey(key: String): PendingProfileImage?

    fun existsByKey(key: String): Boolean

    fun deleteByUserId(userId: Long)
}
