package com.teamnative.moil.domain.group.repository

import com.teamnative.moil.domain.group.model.Group
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : JpaRepository<Group, Long> {
    fun findByInviteCode(inviteCode: String): Group?

    fun existsByInviteCode(inviteCode: String): Boolean
}
