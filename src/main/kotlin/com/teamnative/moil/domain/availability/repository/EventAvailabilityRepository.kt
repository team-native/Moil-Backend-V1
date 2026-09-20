package com.teamnative.moil.domain.availability.repository

import com.teamnative.moil.domain.availability.model.EventAvailability
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EventAvailabilityRepository : JpaRepository<EventAvailability, Long> {
    fun findByEventIdAndUserIdAndAvailableDate(
        eventId: Long,
        userId: Long,
        availableDate: LocalDate,
    ): EventAvailability?

    fun findAllByEventIdAndAvailableDateOrderByUserIdAsc(
        eventId: Long,
        availableDate: LocalDate,
    ): List<EventAvailability>

    fun deleteByEventIdAndUserIdAndAvailableDate(
        eventId: Long,
        userId: Long,
        availableDate: LocalDate,
    )
}
