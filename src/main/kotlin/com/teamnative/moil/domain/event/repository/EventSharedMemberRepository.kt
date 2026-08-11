package com.teamnative.moil.domain.event.repository

import com.teamnative.moil.domain.event.model.EventSharedMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventSharedMemberRepository : JpaRepository<EventSharedMember, Long> {
    fun findAllByEventId(eventId: Long): List<EventSharedMember>

    fun findAllByEventIdIn(eventIds: Collection<Long>): List<EventSharedMember>

    fun deleteByEventId(eventId: Long)

    fun deleteByEventIdIn(eventIds: Collection<Long>)

    fun deleteByUserId(userId: Long)

    @Modifying
    @Query(
        """
        delete from EventSharedMember share
        where share.userId = :userId
          and share.eventId in (
              select event.id
              from Event event
              where event.groupId = :groupId
          )
        """,
    )
    fun deleteByUserIdAndGroupId(userId: Long, groupId: Long)
}
