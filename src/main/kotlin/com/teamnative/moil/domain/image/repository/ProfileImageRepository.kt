package com.teamnative.moil.domain.image.repository

import com.teamnative.moil.domain.image.model.ProfileImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProfileImageRepository : JpaRepository<ProfileImage, Long> {
    fun findByKey(key: String): ProfileImage?

    fun findAllByUserId(userId: Long): List<ProfileImage>

    fun existsByKey(key: String): Boolean

    fun deleteByUserId(userId: Long)
}
