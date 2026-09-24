package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.dto.EventAttendanceMemberResponse
import com.teamnative.moil.domain.event.dto.EventAttendanceSummaryResponse
import com.teamnative.moil.domain.event.dto.EventCalendarResponse
import com.teamnative.moil.domain.event.dto.EventDetailResponse
import com.teamnative.moil.domain.event.dto.EventMemberResponse
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.model.EventAttendanceStatus
import com.teamnative.moil.domain.event.repository.EventAttendanceRepository
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Service
class EventQueryService(
    private val eventRepository: EventRepository,
    private val groupPermissionService: GroupPermissionService,
    private val userAccountRepository: UserAccountRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
    private val eventAttendanceRepository: EventAttendanceRepository,
) {
    @Transactional(readOnly = true)
    fun findGroupCalendar(user: UserAccount, groupId: Long, month: String): List<EventCalendarResponse> {
        val yearMonth = try { YearMonth.parse(month) } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "월 파라미터 형식이 올바르지 않습니다.")
        }
        val zone = ZoneId.of("Asia/Seoul")
        return findGroupCalendar(user, groupId, yearMonth.atDay(1).atStartOfDay(zone).toInstant().toString(), yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toString())
    }

    @Transactional(readOnly = true)
    fun findGroupCalendar(user: UserAccount, groupId: Long, from: String, to: String): List<EventCalendarResponse> {
        groupPermissionService.requireMember(user, groupId)
        val fromInstant = from.toInstant()
        val toInstant = to.toInstant()
        if (fromInstant.isAfter(toInstant)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간이 올바르지 않습니다.")
        return eventRepository.findAllByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(groupId, toInstant, fromInstant)
            .sortedBy { it.startsAt }.map { it.toCalendarResponse(user.id) }
    }

    @Transactional(readOnly = true)
    fun findEvent(user: UserAccount, eventId: Long): EventDetailResponse {
        val event = eventRepository.findById(eventId).orElse(null) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
        groupPermissionService.requireMember(user, event.groupId)
        return event.toDetailResponse(user.id)
    }

    @Transactional(readOnly = true)
    fun findGroupEvent(user: UserAccount, groupId: Long, eventId: Long): EventDetailResponse {
        groupPermissionService.requireMember(user, groupId)
        val event = eventRepository.findByIdAndGroupId(eventId, groupId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
        return event.toDetailResponse(user.id)
    }

    @Transactional(readOnly = true)
    fun findAttendance(user: UserAccount, eventId: Long): EventAttendanceSummaryResponse {
        val event = eventRepository.findById(eventId).orElse(null) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다.")
        groupPermissionService.requireMember(user, event.groupId)
        val members = activeAttendanceMembers(event.id, event.groupId)
        val memberIds = members.map { it.memberId }.toSet()
        val attendances = eventAttendanceRepository.findAllByEventId(event.id).filter { it.userId in memberIds }.associateBy { it.userId }
        return EventAttendanceSummaryResponse(
            myStatus = attendances[user.id]?.status,
            participantCount = members.size,
            attendingCount = attendances.values.count { it.status == EventAttendanceStatus.ATTENDING },
            declinedCount = attendances.values.count { it.status == EventAttendanceStatus.DECLINED },
            members = members.map { it.copy(status = attendances[it.memberId]?.status) },
        )
    }

    private fun String.toInstant(): Instant = try { Instant.parse(this) } catch (exception: DateTimeParseException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "기간 파라미터 형식이 올바르지 않습니다.")
    }

    private fun Event.toDetailResponse(userId: Long): EventDetailResponse = toCalendarShape(userId).let { calendar ->
        EventDetailResponse(calendar.eventId, groupId, calendar.title, calendar.startDate, calendar.endDate, calendar.location, calendar.memo, calendar.members, calendar.myAttendanceStatus, calendar.attendingCount)
    }

    private fun Event.toCalendarResponse(userId: Long): EventCalendarResponse = toCalendarShape(userId)

    private fun Event.toCalendarShape(userId: Long): EventCalendarResponse {
        val zone = ZoneId.of("Asia/Seoul")
        val creator = userAccountRepository.findById(creatorId).orElse(null)
        val creatorProfile = groupMemberRepository.findByGroupIdAndUserId(groupId, creatorId)
        val members = eventMembers(id, groupId)
        val activeMemberIds = members.map { it.userId }.toSet()
        val attendances = eventAttendanceRepository.findAllByEventId(id).filter { it.userId in activeMemberIds }
        return EventCalendarResponse(
            id, title, startsAt.atZone(zone).format(EVENT_DATE_TIME_FORMATTER), endsAt.atZone(zone).format(EVENT_DATE_TIME_FORMATTER), location, memo,
            members.ifEmpty { listOf(EventMemberResponse(creatorId, creator?.name.orEmpty(), creatorProfile?.color ?: DEFAULT_PROFILE_COLOR, creatorProfile?.imagePath)) },
            attendances.firstOrNull { it.userId == userId }?.status,
            attendances.count { it.status == EventAttendanceStatus.ATTENDING },
        )
    }

    private fun activeAttendanceMembers(eventId: Long, groupId: Long): List<EventAttendanceMemberResponse> = eventSharedMemberRepository.findAllByEventId(eventId).mapNotNull { share ->
        val member = groupMemberRepository.findByGroupIdAndUserId(groupId, share.userId) ?: return@mapNotNull null
        EventAttendanceMemberResponse(share.userId, member.nickname, member.color, null)
    }

    private fun eventMembers(eventId: Long, groupId: Long): List<EventMemberResponse> {
        val shares = eventSharedMemberRepository.findAllByEventId(eventId)
        if (shares.isEmpty()) return emptyList()
        val groupMembers = groupMemberRepository.findAllByGroupIdAndUserIdIn(groupId, shares.map { it.userId }).associateBy { it.userId }
        return shares.mapNotNull { share ->
            val member = groupMembers[share.userId] ?: return@mapNotNull null
            EventMemberResponse(share.userId, member.nickname, member.color, member.imagePath)
        }
    }

    companion object {
        private const val DEFAULT_PROFILE_COLOR = "RED"
        private val EVENT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
