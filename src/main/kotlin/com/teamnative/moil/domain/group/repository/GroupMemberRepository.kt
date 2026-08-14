package com.teamnative.moil.domain.group.repository

import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GroupMemberRepository : JpaRepository<GroupMember, Long> {
    fun existsByGroupIdAndUserId(groupId: Long, userId: Long): Boolean

    fun findByGroupIdAndUserId(groupId: Long, userId: Long): GroupMember?

    fun findAllByUserId(userId: Long): List<GroupMember>

    fun findAllByUserIdIn(userIds: Collection<Long>): List<GroupMember>

    fun findAllByGroupId(groupId: Long): List<GroupMember>

    fun findAllByGroupIdAndUserIdIn(groupId: Long, userIds: Collection<Long>): List<GroupMember>

    fun deleteByUserId(userId: Long)

    fun deleteByGroupIdIn(groupIds: Collection<Long>)

    fun countByGroupId(groupId: Long): Long

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update GroupMember member
        set member.color = :color,
            member.imagePath = null
        where member.userId = :userId
          and member.imagePath is not null
        """,
    )
    fun resetImageProfilesByUserId(
        @Param("userId") userId: Long,
        @Param("color") color: String = "RED",
    )

    @Query(
        """
        select member
        from GroupMember member
        where (member.role = :ownerRole and member.ownerGroupId <> member.groupId)
           or (member.role = :ownerRole and member.ownerGroupId is null)
           or (member.role <> :ownerRole and member.ownerGroupId is not null)
        """,
    )
    fun findOwnerGroupIdMismatches(ownerRole: GroupRole = GroupRole.OWNER): List<GroupMember>
}
