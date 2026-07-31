package com.teamnative.moil.domain.event.repository

import com.teamnative.moil.domain.event.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findAllByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(
        groupId: Long,
        to: Instant,
        from: Instant,
    ): List<Event>

    fun countByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(
        groupId: Long,
        to: Instant,
        from: Instant,
    ): Long

    fun findByIdAndGroupId(id: Long, groupId: Long): Event?

    fun findAllByCreatorId(creatorId: Long): List<Event>

    fun findAllByGroupIdIn(groupIds: Collection<Long>): List<Event>

    fun deleteByCreatorId(creatorId: Long)

    fun deleteByGroupIdIn(groupIds: Collection<Long>)
}
