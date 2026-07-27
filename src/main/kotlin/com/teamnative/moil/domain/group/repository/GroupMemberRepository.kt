package com.teamnative.moil.domain.group.repository

import com.teamnative.moil.domain.group.model.GroupMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean

    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?

    fun findAllByUserId(userId: Long): List<GroupMember>

    fun findAllByGroupId(groupId: Long): List<GroupMember>

    fun countByGroupId(groupId: Long): Long
}
