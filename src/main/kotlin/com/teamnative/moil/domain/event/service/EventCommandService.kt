package com.teamnative.moil.domain.event.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.event.dto.EventDetailResponse
import com.teamnative.moil.domain.event.model.Event
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.service.GroupPermissionService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException

@Service
class EventCommandService(
    private val eventRepository: EventRepository,
    private val groupPermissionService: GroupPermissionService,
    private val clock: Clock,
) {

    @Transactional
    fun create(
        user: UserAccount,
        groupId: Long,
        title: String,
        memo: String?,
        startsAt: String,
        endsAt: String,
    ): EventDetailResponse {
        groupPermissionService.requireMember(user, groupId)

        val startsAtInstant = startsAt.toInstant()
        val endsAtInstant = endsAt.toInstant()

        if (!startsAtInstant.isBefore(endsAtInstant)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 시간 범위가 올바르지 않습니다.")
        }

        val now = Instant.now(clock)
        val event = eventRepository.save(
            Event(
                groupId = groupId,
                creatorId = user.id,
                updaterId = user.id,
                title = title,
                memo = memo,
                startsAt = startsAtInstant,
                endsAt = endsAtInstant,
                createdAt = now,
                updatedAt = now,
            ),
        )

        return event.toDetailResponse()
    }

    private fun String.toInstant(): Instant =
        try {
            Instant.parse(this)
        } catch (exception: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "일정 시간 형식이 올바르지 않습니다.")
        }

    private fun Event.toDetailResponse(): EventDetailResponse =
        EventDetailResponse(
            eventId = id,
            groupId = groupId,
            title = title,
            memo = memo,
            startsAt = startsAt,
            endsAt = endsAt,
            creatorId = creatorId,
            updaterId = updaterId,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
