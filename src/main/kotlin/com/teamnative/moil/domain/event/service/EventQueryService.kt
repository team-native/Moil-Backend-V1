package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.dto.EventCalendarResponse
import com.teamnative.moil.domain.event.dto.EventDetailResponse
import com.teamnative.moil.domain.event.dto.EventMemberResponse
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.service.GroupPermissionService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeParseException

@Service
class EventQueryService(
    private val eventRepository: EventRepository,
    private val groupPermissionService: GroupPermissionService,
    private val userAccountRepository: UserAccountRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
) {

    @Transactional(readOnly = true)
    fun findGroupCalendar(user: UserAccount, groupId: Long, month: String): List<EventCalendarResponse> {
        val yearMonth = try {
            YearMonth.parse(month)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "월 파라미터 형식이 올바르지 않습니다.")
        }
        val zone = ZoneId.of("Asia/Seoul")
        val from = yearMonth.atDay(1).atStartOfDay(zone).toInstant()
        val to = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        return findGroupCalendar(user, groupId, from.toString(), to.toString())
    }

    @Transactional(readOnly = true)
    fun findGroupCalendar(user: UserAccount, groupId: Long, from: String, to: String): List<EventCalendarResponse> {
        groupPermissionService.requireMember(user, groupId)

        val fromInstant = from.toInstant()
        val toInstant = to.toInstant()

        if (fromInstant.isAfter(toInstant)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간이 올바르지 않습니다.")
        }

        return eventRepository.findAllByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(
            groupId = groupId,
            to = toInstant,
            from = fromInstant,
        )
            .sortedBy { it.startsAt }
            .map { event ->
                event.toCalendarResponse()
            }
    }

    @Transactional(readOnly = true)
    fun findEvent(user: UserAccount, eventId: Long): EventDetailResponse {
        val event = eventRepository.findById(eventId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")

        groupPermissionService.requireMember(user, event.groupId)

        return event.toDetailResponse()
    }

    @Transactional(readOnly = true)
    fun findGroupEvent(user: UserAccount, groupId: Long, eventId: Long): EventDetailResponse {
        groupPermissionService.requireMember(user, groupId)

        val event = eventRepository.findByIdAndGroupId(eventId, groupId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")

        return event.toDetailResponse()
    }

    private fun String.toInstant(): Instant =
        try {
            Instant.parse(this)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "기간 파라미터 형식이 올바르지 않습니다.")
        }

    private fun Event.toDetailResponse(): EventDetailResponse =
        toCalendarShape().let { calendar ->
            EventDetailResponse(
                eventId = calendar.eventId,
                groupId = groupId,
                title = calendar.title,
                date = calendar.date,
                startTime = calendar.startTime,
                endTime = calendar.endTime,
                location = calendar.location,
                members = calendar.members,
            )
        }

    private fun Event.toCalendarResponse(): EventCalendarResponse = toCalendarShape()

    private fun Event.toCalendarShape(): EventCalendarResponse {
        val zone = ZoneId.of("Asia/Seoul")
        val start = startsAt.atZone(zone)
        val end = endsAt.atZone(zone)
        val isAllDay = start.toLocalTime().toString() == "00:00" &&
            end.toLocalTime().toString() == "00:00" &&
            end.toLocalDate() == start.toLocalDate().plusDays(1)
        val creator = userAccountRepository.findById(creatorId).orElse(null)

        return EventCalendarResponse(
            eventId = id,
            title = title,
            date = start.toLocalDate().toString(),
            startTime = if (isAllDay) null else start.toLocalTime().toString(),
            endTime = if (isAllDay) null else end.toLocalTime().toString(),
            location = location,
            members = eventMembers(id, groupId).ifEmpty {
                listOf(
                    EventMemberResponse(
                        userId = creatorId,
                        nickname = creator?.name.orEmpty(),
                        colorId = DEFAULT_PROFILE_COLOR,
                    ),
                )
            },
        )
    }

    private fun eventMembers(eventId: Long, groupId: Long): List<EventMemberResponse> {
        val shares = eventSharedMemberRepository.findAllByEventId(eventId)
        if (shares.isEmpty()) {
            return emptyList()
        }

        val groupMembers = groupMemberRepository
            .findAllByGroupIdAndUserIdIn(groupId, shares.map { it.userId })
            .associateBy { it.userId }

        return shares.mapNotNull { share ->
            val member = groupMembers[share.userId] ?: return@mapNotNull null
            EventMemberResponse(
                userId = share.userId,
                nickname = member.nickname,
                colorId = member.color,
            )
        }
    }

    companion object {
        private const val DEFAULT_PROFILE_COLOR = "RED"
    }
}
