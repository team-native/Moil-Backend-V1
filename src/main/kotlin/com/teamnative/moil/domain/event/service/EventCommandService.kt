package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.model.EventSharedMember
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.service.GroupPermissionService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Service
class EventCommandService(
    private val eventRepository: EventRepository,
    private val groupPermissionService: GroupPermissionService,
    private val clock: Clock,
    private val groupMemberRepository: GroupMemberRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
) {

    @Transactional
    fun create(
        user: UserAccount,
        groupId: Long,
        title: String,
        date: String,
        isAllDay: Boolean,
        startTime: String?,
        endTime: String?,
        location: String?,
        sharedMemberIds: List<Long>,
    ): Long {
        val range = toRange(date, isAllDay, startTime, endTime)
        val eventId = create(
            user = user,
            groupId = groupId,
            title = title,
            memo = null,
            location = location,
            startsAt = range.first.toString(),
            endsAt = range.second.toString(),
        )
        replaceSharedMembers(eventId, groupId, sharedMemberIds)

        return eventId
    }

    @Transactional
    fun create(
        user: UserAccount,
        groupId: Long,
        title: String,
        memo: String?,
        location: String? = memo,
        startsAt: String,
        endsAt: String,
    ): Long {
        groupPermissionService.requireMember(user, groupId)

        val startsAtInstant = startsAt.toInstant()
        val endsAtInstant = endsAt.toInstant()
        validateRange(startsAtInstant, endsAtInstant)

        val now = Instant.now(clock)
        val event = eventRepository.save(
            Event(
                groupId = groupId,
                creatorId = user.id,
                updaterId = user.id,
                title = title,
                memo = memo,
                location = location,
                startsAt = startsAtInstant,
                endsAt = endsAtInstant,
                createdAt = now,
                updatedAt = now,
            ),
        )

        return event.id
    }

    @Transactional
    fun update(
        user: UserAccount,
        eventId: Long,
        title: String,
        date: String,
        isAllDay: Boolean,
        startTime: String?,
        endTime: String?,
        location: String?,
        sharedMemberIds: List<Long>,
    ) {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.")
        val range = toRange(date, isAllDay, startTime, endTime)

        update(
            user = user,
            groupId = event.groupId,
            eventId = eventId,
            title = title,
            memo = event.memo,
            location = location,
            startsAt = range.first.toString(),
            endsAt = range.second.toString(),
        )
        replaceSharedMembers(eventId, event.groupId, sharedMemberIds)
    }

    @Transactional
    fun update(
        user: UserAccount,
        groupId: Long,
        eventId: Long,
        title: String,
        memo: String?,
        location: String? = memo,
        startsAt: String,
        endsAt: String,
    ) {
        groupPermissionService.requireMember(user, groupId)

        val event = eventRepository.findByIdAndGroupId(eventId, groupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.")
        val startsAtInstant = startsAt.toInstant()
        val endsAtInstant = endsAt.toInstant()
        validateRange(startsAtInstant, endsAtInstant)

        eventRepository.save(
            event.copy(
                updaterId = user.id,
                title = title,
                memo = memo,
                location = location,
                startsAt = startsAtInstant,
                endsAt = endsAtInstant,
                updatedAt = Instant.now(clock),
            ),
        )
    }

    @Transactional
    fun delete(user: UserAccount, eventId: Long) {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.")

        delete(user, event.groupId, eventId)
    }

    @Transactional
    fun delete(user: UserAccount, groupId: Long, eventId: Long) {
        groupPermissionService.requireMember(user, groupId)

        val event = eventRepository.findByIdAndGroupId(eventId, groupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found.")

        eventSharedMemberRepository.deleteByEventId(event.id)
        eventRepository.delete(event)
    }

    private fun replaceSharedMembers(eventId: Long, groupId: Long, userIds: List<Long>) {
        val groupMemberUserIds = groupMemberRepository.findAllByGroupId(groupId).map { it.userId }.toSet()
        val distinctUserIds = userIds.distinct()

        if (distinctUserIds.any { it !in groupMemberUserIds }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Shared members must belong to the group.")
        }

        eventSharedMemberRepository.deleteByEventId(eventId)
        eventSharedMemberRepository.saveAll(
            distinctUserIds.map { userId ->
                EventSharedMember(
                    eventId = eventId,
                    userId = userId,
                    createdAt = Instant.now(clock),
                )
            },
        )
    }

    private fun String.toInstant(): Instant =
        try {
            Instant.parse(this)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event time format.")
        }

    private fun toRange(
        date: String,
        isAllDay: Boolean,
        startTime: String?,
        endTime: String?,
    ): Pair<Instant, Instant> {
        val day = try {
            LocalDate.parse(date)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event date format.")
        }
        val zone = ZoneId.of("Asia/Seoul")

        if (isAllDay) {
            return day.atStartOfDay(zone).toInstant() to day.plusDays(1).atStartOfDay(zone).toInstant()
        }

        val start = parseTime(startTime)
        val end = parseTime(endTime)

        return day.atTime(start).atZone(zone).toInstant() to day.atTime(end).atZone(zone).toInstant()
    }

    private fun parseTime(value: String?): LocalTime =
        try {
            LocalTime.parse(value)
        } catch (exception: RuntimeException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event time format.")
        }

    private fun validateRange(startsAt: Instant, endsAt: Instant) {
        if (!startsAt.isBefore(endsAt)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid event time range.")
        }
    }
}
