package com.teamnative.moil.domain.event.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.event.dto.CreateEventRequest
import com.teamnative.moil.domain.event.dto.EventCalendarResponse
import com.teamnative.moil.domain.event.dto.EventDetailResponse
import com.teamnative.moil.domain.event.service.EventCommandService
import com.teamnative.moil.domain.event.service.EventQueryService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val eventQueryService: EventQueryService,
    private val eventCommandService: EventCommandService,
) {

    @GetMapping
    fun events(): ApiResponse<Nothing> = ApiResponse.empty("/events")

    @GetMapping("/groups/{groupId:[0-9]+}/calendar")
    fun groupCalendar(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @RequestParam from: String,
        @RequestParam to: String,
    ): ApiResponse<List<EventCalendarResponse>> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 캘린더를 조회했습니다.",
            data = eventQueryService.findGroupCalendar(user, groupId, from, to),
        )
    }

    @PostMapping("/groups/{groupId:[0-9]+}")
    fun createGroupEvent(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateEventRequest,
    ): ApiResponse<EventDetailResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 일정이 추가되었습니다.",
            data = eventCommandService.create(
                user = user,
                groupId = groupId,
                title = request.title,
                memo = request.memo,
                startsAt = request.startsAt,
                endsAt = request.endsAt,
            ),
        )
    }

    @GetMapping("/groups/{groupId:[0-9]+}/{eventId:[0-9]+}")
    fun groupEvent(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long,
    ): ApiResponse<EventDetailResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 일정을 조회했습니다.",
            data = eventQueryService.findGroupEvent(user, groupId, eventId),
        )
    }
}
