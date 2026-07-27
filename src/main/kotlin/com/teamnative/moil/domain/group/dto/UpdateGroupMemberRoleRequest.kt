package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole
import jakarta.validation.constraints.NotNull

data class UpdateGroupMemberRoleRequest(
    @field:NotNull(message = "변경할 권한을 입력해주세요.")
    val role: GroupRole?,
)
