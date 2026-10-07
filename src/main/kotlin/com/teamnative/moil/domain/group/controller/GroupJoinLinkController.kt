package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.global.config.AppLinkProperties
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class GroupJoinLinkController(
    private val appLinkProperties: AppLinkProperties,
    private val groupRepository: GroupRepository,
) {

    @GetMapping("/join/{groupId:[0-9]+}")
    fun joinLink(@PathVariable groupId: Long): ResponseEntity<Void> {
        val group = groupRepository.findById(groupId).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, appLinkProperties.groupJoinUri(group.id, group.inviteCode))
            .build()
    }
}
