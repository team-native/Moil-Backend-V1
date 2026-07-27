package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.event.dto.EventCalendarResponse
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.service.GroupPermissionService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.format.DateTimeParseException

@Service
class EventQueryService(
    private val eventRepository: EventRepository,
    private val groupPermissionService: GroupPermissionService,
) {

    @Transactional(readOnly = true)
    fun findGroupCalendar(user: UserAccount, groupId: Long, from: String, to: String): List<EventCalendarResponse> {
        groupPermissionService.requireMember(user, groupId)

        val fromInstant = from.toInstant()
        val toInstant = to.toInstant()

        if (fromInstant.isAfter(toInstant)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 기간이 올바르지 않습니다.")
        }

        return eventRepository.findAllByGroupIdAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
            groupId = groupId,
            to = toInstant,
            from = fromInstant,
        )
            .sortedBy { it.startsAt }
            .map { event ->
                EventCalendarResponse(
                    eventId = event.id,
                    title = event.title,
                    startsAt = event.startsAt,
                    endsAt = event.endsAt,
                )
            }
    }

    private fun String.toInstant(): Instant =
        try {
            Instant.parse(this)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "기간 파라미터가 올바르지 않습니다.")
        }
}
