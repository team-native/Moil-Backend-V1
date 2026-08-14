package com.teamnative.moil.domain.group.controller

import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.event.dto.EventCalendarResponse
import com.teamnative.moil.domain.event.service.EventQueryService
import com.teamnative.moil.domain.group.dto.CheckGroupInviteRequest
import com.teamnative.moil.domain.group.dto.CheckGroupInviteResponse
import com.teamnative.moil.domain.group.dto.CreateGroupRequest
import com.teamnative.moil.domain.group.dto.CreateGroupResponse
import com.teamnative.moil.domain.group.dto.GroupDetailResponse
import com.teamnative.moil.domain.group.dto.GroupMemberResponse
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.dto.JoinGroupRequest
import com.teamnative.moil.domain.group.dto.JoinGroupResponse
import com.teamnative.moil.domain.group.dto.TransferGroupOwnerRequest
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberRoleRequest
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberProfileRequest
import com.teamnative.moil.domain.group.dto.UpdateGroupMemberProfileResponse
import com.teamnative.moil.domain.group.dto.UpdateGroupNameRequest
import com.teamnative.moil.domain.group.dto.UpdateGroupNotificationRequest
import com.teamnative.moil.domain.group.dto.UpdateGroupNotificationResponse
import com.teamnative.moil.domain.group.service.GroupCreateService
import com.teamnative.moil.domain.group.service.GroupInviteService
import com.teamnative.moil.domain.group.service.GroupManagementService
import com.teamnative.moil.domain.group.service.GroupMemberQueryService
import com.teamnative.moil.domain.group.service.GroupMemberProfileService
import com.teamnative.moil.domain.group.service.GroupNotificationService
import com.teamnative.moil.domain.group.service.GroupQueryService
import com.teamnative.moil.domain.group.service.ProfileSelectionValidator
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/groups")
class GroupController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val groupQueryService: GroupQueryService,
    private val groupCreateService: GroupCreateService,
    private val groupInviteService: GroupInviteService,
    private val groupMemberQueryService: GroupMemberQueryService,
    private val groupMemberProfileService: GroupMemberProfileService,
    private val groupNotificationService: GroupNotificationService,
    private val groupManagementService: GroupManagementService,
    private val eventQueryService: EventQueryService,
    private val profileSelectionValidator: ProfileSelectionValidator,
) {

    @PostMapping("/me")
    fun groups(
        @RequestHeader("Authorization", required = false) authorization: String?,
    ): ApiResponse<List<GroupSummaryResponse>> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "내 그룹 목록을 조회했습니다.",
            data = groupQueryService.findMyGroups(user),
        )
    }

    @PostMapping
    fun create(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: CreateGroupRequest,
    ): ApiResponse<CreateGroupResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        val nickname = requireNickname(request.nickname)
        val profile = profileSelectionValidator.requireProfileSelection(user, request.colorId, request.imagePath)

        return ApiResponse.success(
            message = "그룹을 생성했습니다.",
            data = groupCreateService.create(user, request.name, nickname, profile),
        )
    }

    @PostMapping("/join/verify")
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
        val nickname = requireNickname(request.nickname)
        val color = requireColor(request.colorId)

        return ApiResponse.success(
            message = "그룹에 참여했습니다.",
            data = groupInviteService.join(user, request.inviteCode, nickname, color),
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

    @DeleteMapping("/{groupId:[0-9]+}/members/me")
    fun leave(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        groupManagementService.leave(user, groupId)

        return ApiResponse.empty("그룹에서 퇴장했습니다.")
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

    @PatchMapping("/{groupId:[0-9]+}/notification")
    fun updateNotification(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupNotificationRequest,
    ): ApiResponse<UpdateGroupNotificationResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 알림 설정을 변경했습니다.",
            data = groupNotificationService.update(user, groupId, request.enabled!!),
        )
    }

    @PatchMapping("/{groupId:[0-9]+}/members/me")
    fun updateMyProfile(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupMemberProfileRequest,
    ): ApiResponse<UpdateGroupMemberProfileResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        val nickname = requireNickname(request.nickname)
        val color = requireColor(request.colorId)

        return ApiResponse.success(
            message = "프로필이 변경되었습니다.",
            data = groupMemberProfileService.update(user, groupId, nickname, color),
        )
    }

    @GetMapping("/{groupId:[0-9]+}/events")
    fun groupCalendar(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @RequestParam month: String,
    ): ApiResponse<List<EventCalendarResponse>> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "그룹 캘린더를 조회했습니다.",
            data = eventQueryService.findGroupCalendar(user, groupId, month),
        )
    }

    @PatchMapping("/{groupId:[0-9]+}")
    fun updateName(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupNameRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        groupManagementService.updateName(user, groupId, request.name)

        return ApiResponse.empty("그룹 이름을 변경했습니다.")
    }

    @PatchMapping("/{groupId:[0-9]+}/members")
    fun updateMemberRole(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: UpdateGroupMemberRoleRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        request.members.forEach { member ->
            groupManagementService.updateMemberRoleByUserId(user, groupId, member.userId, member.role)
        }

        return ApiResponse.empty("멤버 권한을 변경했습니다.")
    }

    @PostMapping("/{groupId:[0-9]+}/transfer-admin")
    fun transferOwner(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: TransferGroupOwnerRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        groupManagementService.transferOwnerToUser(user, groupId, request.targetUserId!!)

        return ApiResponse.empty("관리자 권한을 이전했습니다.")
    }

    private fun requireNickname(nickname: String?): String {
        val value = nickname?.trim().orEmpty()
        if (value.length !in 1..10) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "닉네임은 1자 이상 10자 이하로 입력해야 합니다.")
        }

        return value
    }

    private fun requireColor(color: String?): String {
        val value = color?.trim().orEmpty()
        if (value.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "프로필 색상을 선택해야 합니다.")
        }

        return value
    }
}
