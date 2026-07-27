package com.teamnative.moil.domain.event.repository

import com.teamnative.moil.domain.event.model.Event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EventRepository : JpaRepository<Event, Long> {
    fun findAllByGroupIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
        groupId: Long,
        to: Instant,
        from: Instant,
    ): List<Event>

    fun findByIdAndGroupId(id: Long, groupId: Long): Event?
}
