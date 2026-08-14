package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.global.config.AppLinkProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupJoinLinkController(
    private val appLinkProperties: AppLinkProperties,
) {

    @GetMapping("/join/{groupId:[0-9]+}")
    fun joinLink(@PathVariable groupId: Long): ResponseEntity<Void> =
        ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, appLinkProperties.groupJoinUri(groupId))
            .build()
}
