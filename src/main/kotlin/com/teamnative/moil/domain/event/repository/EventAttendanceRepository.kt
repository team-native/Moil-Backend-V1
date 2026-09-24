package com.teamnative.moil.domain.event.repository

import com.teamnative.moil.domain.event.model.EventAttendance
import org.springframework.data.jpa.repository.JpaRepository

interface EventAttendanceRepository : JpaRepository<EventAttendance, Long> {
    fun findByEventIdAndUserId(eventId: Long, userId: Long): EventAttendance?

    fun findAllByEventId(eventId: Long): List<EventAttendance>

    fun findAllByEventIdIn(eventIds: Collection<Long>): List<EventAttendance>

    fun deleteByEventId(eventId: Long)

    fun deleteByEventIdIn(eventIds: Collection<Long>)

    fun deleteByUserId(userId: Long)

    fun deleteByUserIdAndEventIdIn(userId: Long, eventIds: Collection<Long>)
}
