package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/groups")
class GroupController {

    @GetMapping
    fun groups(): ApiResponse<Nothing> = ApiResponse.empty("/groups")
}
