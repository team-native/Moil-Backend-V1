package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.dto.GroupDetailMemberResponse
import com.teamnative.moil.domain.group.dto.GroupDetailResponse
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

@Service
class GroupQueryService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val groupPermissionService: GroupPermissionService,
    private val userAccountRepository: UserAccountRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun findMyGroups(user: UserAccount): List<GroupSummaryResponse> {
        val members = groupMemberRepository.findAllByUserId(user.id)
        val groups = groupRepository.findAllById(members.map { it.groupId })
            .associateBy { it.id }

        return members.mapNotNull { member ->
            val group = groups[member.groupId] ?: return@mapNotNull null

            GroupSummaryResponse(
                groupId = group.id,
                name = group.name,
                inviteCode = group.inviteCode,
                myRole = member.role.toApiRole(),
                myNickname = member.nickname,
                myColor = member.color,
                memberCount = groupMemberRepository.countByGroupId(group.id),
            )
        }
    }

    @Transactional(readOnly = true)
    fun findGroup(user: UserAccount, groupId: Long): GroupDetailResponse {
        val access = groupPermissionService.requireMember(user, groupId)
        val members = groupMemberRepository.findAllByGroupId(groupId).sortedBy { it.joinedAt }
        val users = userAccountRepository.findAllById(members.map { it.userId }).associateBy { it.id }
        val zone = ZoneId.of("Asia/Seoul")
        val currentMonth = YearMonth.now(clock.withZone(zone))
        val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant()
        val nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        return GroupDetailResponse(
            groupId = access.group.id,
            name = access.group.name,
            inviteCode = access.group.inviteCode,
            memberCount = groupMemberRepository.countByGroupId(access.group.id),
            monthlyEventCount = eventRepository.countByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(
                groupId = access.group.id,
                to = nextMonthStart,
                from = monthStart,
            ),
            myRole = access.member.role.toApiRole(),
            members = members.mapNotNull { member ->
                val memberUser = users[member.userId] ?: return@mapNotNull null
                GroupDetailMemberResponse(
                    userId = member.userId,
                    nickname = member.nickname,
                    role = member.role.toApiRole(),
                    color = member.color,
                )
            },
        )
    }
}
