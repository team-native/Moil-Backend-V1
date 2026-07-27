package com.teamnative.moil.domain.group.dto

import com.teamnative.moil.domain.group.model.GroupRole

data class TransferGroupOwnerResponse(
    val groupId: Long,
    val previousOwnerMemberId: Long,
    val previousOwnerRole: GroupRole,
    val newOwnerMemberId: Long,
    val newOwnerRole: GroupRole,
)
