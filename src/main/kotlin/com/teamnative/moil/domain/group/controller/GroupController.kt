package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.service.GroupQueryService
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/groups")
class GroupController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val groupQueryService: GroupQueryService,
) {

    @GetMapping
    fun groups(
        @RequestHeader("Authorization", required = false) authorization: String?,
    ): ApiResponse<List<GroupSummaryResponse>> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "내 그룹 목록을 조회했습니다.",
            data = groupQueryService.findMyGroups(user),
        )
    }
}
