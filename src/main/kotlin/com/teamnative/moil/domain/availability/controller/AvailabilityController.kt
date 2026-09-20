package com.teamnative.moil.domain.availability.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.availability.dto.SaveAvailabilityRequest
import com.teamnative.moil.domain.availability.service.AvailabilityCommandService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events/{eventId:[0-9]+}/availability")
class AvailabilityController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val availabilityCommandService: AvailabilityCommandService,
) {

    @PutMapping
    fun save(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable eventId: Long,
        @Valid @RequestBody request: SaveAvailabilityRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        availabilityCommandService.save(
            userId = user.id,
            eventId = eventId,
            date = request.date,
            timeSlots = request.timeSlots,
        )

        return ApiResponse.empty("가능한 시간대를 저장했습니다.")
    }

    @DeleteMapping
    fun delete(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable eventId: Long,
        @RequestParam date: String,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        availabilityCommandService.delete(
            userId = user.id,
            eventId = eventId,
            date = date,
        )

        return ApiResponse.empty("가능한 시간대를 삭제했습니다.")
    }
}
