package com.teamnative.moil.domain.event.repository

import com.teamnative.moil.domain.event.model.EventSharedMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventSharedMemberRepository : JpaRepository<EventSharedMember, Long> {
    fun findAllByEventId(eventId: Long): List<EventSharedMember>

    fun findAllByEventIdIn(eventIds: Collection<Long>): List<EventSharedMember>

    fun deleteByEventId(eventId: Long)

    fun deleteByEventIdIn(eventIds: Collection<Long>)

    fun deleteByUserId(userId: Long)
}
