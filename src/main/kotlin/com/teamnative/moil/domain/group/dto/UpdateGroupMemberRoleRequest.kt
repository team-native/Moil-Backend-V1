package com.teamnative.moil.domain.group.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class UpdateGroupMemberRoleRequest(
    @field:NotEmpty(message = "변경할 멤버 목록을 입력해주세요.")
    @field:Valid
    val members: List<GroupMemberRoleChange>,
)

data class GroupMemberRoleChange(
    @field:NotNull(message = "대상 멤버를 입력해주세요.")
    val userId: Long,

    @field:NotNull(message = "변경할 권한을 입력해주세요.")
    val role: String,
)
