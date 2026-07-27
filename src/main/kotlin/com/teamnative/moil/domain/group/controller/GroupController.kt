package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.group.dto.CheckGroupInviteRequest
import com.teamnative.moil.domain.group.dto.CheckGroupInviteResponse
import com.teamnative.moil.domain.group.dto.CreateGroupRequest
import com.teamnative.moil.domain.group.dto.CreateGroupResponse
import com.teamnative.moil.domain.group.dto.GroupDetailResponse
import com.teamnative.moil.domain.group.dto.GroupMemberResponse
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.dto.JoinGroupRequest
import com.teamnative.moil.domain.group.dto.JoinGroupResponse
import com.teamnative.moil.domain.group.service.GroupCreateService
import com.teamnative.moil.domain.group.service.GroupInviteService
import com.teamnative.moil.domain.group.service.GroupMemberQueryService
import com.teamnative.moil.domain.group.service.GroupQueryService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/groups")
class GroupController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val groupQueryService: GroupQueryService,
    private val groupCreateService: GroupCreateService,
    private val groupInviteService: GroupInviteService,
    private val groupMemberQueryService: GroupMemberQueryService,
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

    @GetMapping("/{groupId:[0-9]+}")
    fun group(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
    ): ApiResponse<GroupDetailResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 정보를 조회했습니다.",
            data = groupQueryService.findGroup(user, groupId),
        )
    }

    @GetMapping("/{groupId:[0-9]+}/members")
    fun members(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
    ): ApiResponse<List<GroupMemberResponse>> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 멤버 목록을 조회했습니다.",
            data = groupMemberQueryService.findMembers(user, groupId),
        )
    }

    @PostMapping
    fun create(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ApiResponse<CreateGroupResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹이 생성되었습니다.",
            data = groupCreateService.create(user, request.name),
        )
    }

    @PostMapping("/invite/check")
    fun checkInvite(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: CheckGroupInviteRequest,
    ): ApiResponse<CheckGroupInviteResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "참가 가능한 그룹입니다.",
            data = groupInviteService.check(user, request.inviteCode),
        )
    }

    @PostMapping("/join")
    fun join(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: JoinGroupRequest,
    ): ApiResponse<JoinGroupResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹에 참가했습니다.",
            data = groupInviteService.join(user, request.inviteCode),
        )
    }
}
