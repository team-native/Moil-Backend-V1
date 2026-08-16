package com.teamnative.moil.domain.event.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.event.dto.CreateEventResponse
import com.teamnative.moil.domain.event.dto.CreateEventRequest
import com.teamnative.moil.domain.event.dto.EventDetailResponse
import com.teamnative.moil.domain.event.dto.UpdateEventRequest
import com.teamnative.moil.domain.event.service.EventCommandService
import com.teamnative.moil.domain.event.service.EventQueryService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/events")
class EventController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val eventQueryService: EventQueryService,
    private val eventCommandService: EventCommandService,
) {

    @PostMapping
    fun create(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: CreateEventRequest,
    ): ApiResponse<CreateEventResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        if (request.sharedMemberIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "일정을 공유할 멤버를 1명 이상 선택해야 합니다.")
        }
        val eventId = eventCommandService.create(
            user = user,
            groupId = request.groupId!!,
            title = request.title,
            startDate = request.startDate,
            endDate = request.endDate,
            location = request.location,
            memo = request.memo,
            sharedMemberIds = request.sharedMemberIds,
        )

        return ApiResponse.success(
            message = "일정을 등록했습니다.",
            data = CreateEventResponse(eventId),
        )
    }

    @GetMapping("/{eventId:[0-9]+}")
    fun event(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable eventId: Long,
    ): ApiResponse<EventDetailResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 일정을 조회했습니다.",
            data = eventQueryService.findEvent(user, eventId),
        )
    }

    @PatchMapping("/{eventId:[0-9]+}")
    fun update(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable eventId: Long,
        @Valid @RequestBody request: UpdateEventRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        if (request.sharedMemberIds.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "일정을 공유할 멤버를 1명 이상 선택해야 합니다.")
        }
        eventCommandService.update(
            user = user,
            eventId = eventId,
            title = request.title,
            startDate = request.startDate,
            endDate = request.endDate,
            location = request.location,
            memo = request.memo,
            sharedMemberIds = request.sharedMemberIds,
        )

        return ApiResponse.empty("일정을 수정했습니다.")
    }

    @DeleteMapping("/{eventId:[0-9]+}")
    fun delete(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable eventId: Long,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        eventCommandService.delete(user, eventId)

        return ApiResponse.empty("일정을 삭제했습니다.")
    }
}
