package com.teamnative.moil.domain.availability.service

import com.teamnative.moil.domain.availability.dto.AvailabilitySlotRequest
import com.teamnative.moil.domain.availability.model.EventAvailability
import com.teamnative.moil.domain.availability.model.EventAvailabilitySlot
import com.teamnative.moil.domain.availability.repository.EventAvailabilityRepository
import com.teamnative.moil.domain.availability.repository.EventAvailabilitySlotRepository
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Service
class AvailabilityCommandService(
    private val eventRepository: EventRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
    private val eventAvailabilityRepository: EventAvailabilityRepository,
    private val eventAvailabilitySlotRepository: EventAvailabilitySlotRepository,
    private val clock: Clock,
) {

    @Transactional
    fun save(
        userId: Long,
        eventId: Long,
        date: String,
        timeSlots: List<AvailabilitySlotRequest>,
    ) {
        requireParticipant(userId, eventId)

        val availableDate = parseDate(date)
        val ranges = timeSlots.map { request ->
            val startTime = parseTime(request.startTime, "시작 시간")
            val endTime = parseTime(request.endTime, "종료 시간")
            if (!startTime.isBefore(endTime)) {
                throw badRequest("시작 시간은 종료 시간보다 빨라야 합니다.")
            }
            AvailabilityRange(startTime, endTime)
        }.let(::mergeRanges)

        val existing = eventAvailabilityRepository.findByEventIdAndUserIdAndAvailableDate(
            eventId = eventId,
            userId = userId,
            availableDate = availableDate,
        )
        val now = Instant.now(clock)
        val availability = eventAvailabilityRepository.save(
            existing?.copy(updatedAt = now)
                ?: EventAvailability(
                    eventId = eventId,
                    userId = userId,
                    availableDate = availableDate,
                    createdAt = now,
                    updatedAt = now,
                ),
        )

        eventAvailabilitySlotRepository.deleteAllByAvailabilityId(availability.id)
        eventAvailabilitySlotRepository.saveAll(
            ranges.map { range ->
                EventAvailabilitySlot(
                    availabilityId = availability.id,
                    startsAt = toInstant(availableDate, range.startTime),
                    endsAt = toInstant(availableDate, range.endTime),
                )
            },
        )
    }

    @Transactional
    fun delete(userId: Long, eventId: Long, date: String) {
        requireParticipant(userId, eventId)

        val availableDate = parseDate(date)
        val availability = eventAvailabilityRepository.findByEventIdAndUserIdAndAvailableDate(
            eventId = eventId,
            userId = userId,
            availableDate = availableDate,
        ) ?: return

        eventAvailabilitySlotRepository.deleteAllByAvailabilityId(availability.id)
        eventAvailabilityRepository.delete(availability)
    }

    private fun requireParticipant(userId: Long, eventId: Long) {
        eventRepository.findById(eventId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
        }

        val isParticipant = eventSharedMemberRepository.findAllByEventId(eventId)
            .any { it.userId == userId }
        if (!isParticipant) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "해당 일정의 참여자가 아닙니다.")
        }
    }

    private fun parseDate(value: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (exception: DateTimeParseException) {
        throw badRequest("가능 날짜는 yyyy-MM-dd 형식으로 입력해주세요.")
    }

    private fun parseTime(value: String, fieldName: String): LocalTime = try {
        LocalTime.parse(value)
    } catch (exception: DateTimeParseException) {
        throw badRequest("$fieldName 형식이 올바르지 않습니다.")
    }

    private fun toInstant(date: LocalDate, time: LocalTime): Instant =
        LocalDateTime.of(date, time).atZone(ZONE_ID).toInstant()

    private fun mergeRanges(ranges: List<AvailabilityRange>): List<AvailabilityRange> {
        if (ranges.isEmpty()) {
            return emptyList()
        }

        val sorted = ranges
            .sortedWith(compareBy<AvailabilityRange> { it.startTime }.thenBy { it.endTime })

        return sorted.drop(1).fold(mutableListOf(sorted.first())) { merged, current ->
                val previous = merged.last()
                if (current.startTime <= previous.endTime) {
                    merged[merged.lastIndex] = previous.copy(
                        endTime = maxOf(previous.endTime, current.endTime),
                    )
                } else {
                    merged += current
                }
                merged
            }
    }

    private fun badRequest(message: String) =
        ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    private data class AvailabilityRange(
        val startTime: LocalTime,
        val endTime: LocalTime,
    )

    companion object {
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
