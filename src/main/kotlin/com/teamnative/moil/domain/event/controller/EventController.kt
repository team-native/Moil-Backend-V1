package com.teamnative.moil.domain.event.controller

import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/events")
class EventController {

    @GetMapping
    fun events(): ApiResponse<Nothing> = ApiResponse.empty("/events")
}
