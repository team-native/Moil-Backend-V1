package com.teamnative.moil.domain.availability.repository

import com.teamnative.moil.domain.availability.model.EventAvailabilitySlot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventAvailabilitySlotRepository : JpaRepository<EventAvailabilitySlot, Long> {
    fun findAllByAvailabilityIdInOrderByStartsAtAsc(availabilityIds: Collection<Long>): List<EventAvailabilitySlot>

    fun deleteAllByAvailabilityId(availabilityId: Long)
}
