package com.teamnative.moil.domain.availability.service

import com.teamnative.moil.domain.availability.dto.AvailabilityMemberResponse
import com.teamnative.moil.domain.availability.dto.AvailabilityResponse
import com.teamnative.moil.domain.availability.dto.AvailabilitySummaryResponse
import com.teamnative.moil.domain.availability.dto.AvailabilitySummarySlotResponse
import com.teamnative.moil.domain.availability.dto.AvailabilityTimeSlotResponse
import com.teamnative.moil.domain.availability.dto.MyAvailabilityResponse
import com.teamnative.moil.domain.availability.repository.EventAvailabilityRepository
import com.teamnative.moil.domain.availability.repository.EventAvailabilitySlotRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class AvailabilityQueryService(
    private val eventRepository: EventRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
    private val eventAvailabilityRepository: EventAvailabilityRepository,
    private val eventAvailabilitySlotRepository: EventAvailabilitySlotRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val userAccountRepository: UserAccountRepository,
    private val availabilityCalculator: AvailabilityCalculator,
) {

    @Transactional(readOnly = true)
    fun findMine(userId: Long, eventId: Long, date: String): MyAvailabilityResponse {
        requireParticipant(userId, eventId)
        val availableDate = parseDate(date)
        val availability = eventAvailabilityRepository.findByEventIdAndUserIdAndAvailableDate(
            eventId = eventId,
            userId = userId,
            availableDate = availableDate,
        )
        val slots = availability?.let { slotsByAvailability(listOf(it.id))[it.id].orEmpty() }.orEmpty()

        return MyAvailabilityResponse(
            eventId = eventId,
            date = availableDate.toString(),
            userId = userId,
            timeSlots = slots.map(::toTimeSlotResponse),
        )
    }

    @Transactional(readOnly = true)
    fun findAll(userId: Long, eventId: Long, date: String): AvailabilityResponse {
        val event = requireParticipant(userId, eventId)
        val availableDate = parseDate(date)
        val shares = eventSharedMemberRepository.findAllByEventId(eventId)
        val availabilities = eventAvailabilityRepository.findAllByEventIdAndAvailableDateOrderByUserIdAsc(
            eventId = eventId,
            availableDate = availableDate,
        )
        val slotsByAvailability = slotsByAvailability(availabilities.map { it.id })
        val profiles = groupMemberRepository
            .findAllByGroupIdAndUserIdIn(event.groupId, shares.map { it.userId })
            .associateBy { it.userId }
        val users = userAccountRepository
            .findAllById(shares.map { it.userId })
            .associateBy { it.id }
        val availabilityByUser = availabilities.associateBy { it.userId }

        return AvailabilityResponse(
            eventId = eventId,
            date = availableDate.toString(),
            members = shares.map { share ->
                val profile = profiles[share.userId]
                val availability = availabilityByUser[share.userId]
                AvailabilityMemberResponse(
                    userId = share.userId,
                    nickname = profile?.nickname ?: users[share.userId]?.name.orEmpty(),
                    colorId = profile?.color,
                    timeSlots = availability
                        ?.let { slotsByAvailability[it.id].orEmpty() }
                        .orEmpty()
                        .map(::toTimeSlotResponse),
                )
            },
        )
    }

    @Transactional(readOnly = true)
    fun findSummary(userId: Long, eventId: Long, date: String): AvailabilitySummaryResponse {
        val event = requireParticipant(userId, eventId)
        val availableDate = parseDate(date)
        val shares = eventSharedMemberRepository.findAllByEventId(eventId)
        val availabilities = eventAvailabilityRepository.findAllByEventIdAndAvailableDateOrderByUserIdAsc(
            eventId = eventId,
            availableDate = availableDate,
        )
        val slotsByAvailability = slotsByAvailability(availabilities.map { it.id })
        val userAvailabilities = availabilities.map { availability ->
            UserAvailability(
                userId = availability.userId,
                timeSlots = slotsByAvailability[availability.id].orEmpty().map {
                    AvailabilityTimeRange(
                        startTime = it.startsAt.atZone(ZONE_ID).toLocalTime(),
                        endTime = it.endsAt.atZone(ZONE_ID).toLocalTime(),
                    )
                },
            )
        }
        val participantCount = shares.size
        val calculated = availabilityCalculator.calculate(userAvailabilities)

        return AvailabilitySummaryResponse(
            eventId = eventId,
            date = availableDate.toString(),
            participantCount = participantCount,
            respondedCount = availabilities.size,
            timeSlots = calculated.map { slot ->
                AvailabilitySummarySlotResponse(
                    startTime = formatTime(slot.startTime),
                    endTime = formatTime(slot.endTime),
                    availableCount = slot.availableCount,
                    availableMemberIds = slot.availableMemberIds,
                    isAvailableForEveryone = participantCount > 0 &&
                        slot.availableCount == participantCount,
                )
            },
        )
    }

    private fun slotsByAvailability(availabilityIds: Collection<Long>) =
        if (availabilityIds.isEmpty()) {
            emptyMap()
        } else {
            eventAvailabilitySlotRepository
                .findAllByAvailabilityIdInOrderByStartsAtAsc(availabilityIds)
                .groupBy { it.availabilityId }
        }

    private fun toTimeSlotResponse(slot: com.teamnative.moil.domain.availability.model.EventAvailabilitySlot) =
        AvailabilityTimeSlotResponse(
            startTime = formatTime(slot.startsAt.atZone(ZONE_ID).toLocalTime()),
            endTime = formatTime(slot.endsAt.atZone(ZONE_ID).toLocalTime()),
        )

    private fun requireParticipant(userId: Long, eventId: Long) =
        eventRepository.findById(eventId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
        }.also {
            val isParticipant = eventSharedMemberRepository.findAllByEventId(eventId)
                .any { share -> share.userId == userId }
            if (!isParticipant) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "해당 일정의 참여자가 아닙니다.")
            }
        }

    private fun parseDate(value: String): LocalDate = try {
        LocalDate.parse(value)
    } catch (exception: DateTimeParseException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "가능 날짜는 yyyy-MM-dd 형식으로 입력해주세요.")
    }

    private fun formatTime(time: LocalTime): String = time.format(TIME_FORMATTER)

    companion object {
        private val ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
