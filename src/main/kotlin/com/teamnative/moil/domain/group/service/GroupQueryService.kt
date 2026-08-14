package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.dto.GroupDetailMemberResponse
import com.teamnative.moil.domain.group.dto.GroupDetailResponse
import com.teamnative.moil.domain.group.dto.GroupSummaryResponse
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

@Service
class GroupQueryService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
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
                myColor = member.color ?: DEFAULT_PROFILE_COLOR,
                memberCount = groupMemberRepository.countByGroupId(group.id),
            )
        }
    }

    @Transactional(readOnly = true)
    fun findGroup(user: UserAccount, groupId: Long): GroupDetailResponse {
        val group = groupRepository.findById(groupId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "洹몃９??李얠쓣 ???놁뒿?덈떎.")
        val currentUserMember = groupMemberRepository.findByGroupIdAndUserId(groupId, user.id)
        val members = groupMemberRepository.findAllByGroupId(groupId).sortedBy { it.joinedAt }
        val users = userAccountRepository.findAllById(members.map { it.userId }).associateBy { it.id }
        val zone = ZoneId.of("Asia/Seoul")
        val currentMonth = YearMonth.now(clock.withZone(zone))
        val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant()
        val nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()

        return GroupDetailResponse(
            groupId = group.id,
            name = group.name,
            inviteCode = group.inviteCode,
            memberCount = groupMemberRepository.countByGroupId(group.id),
            monthlyEventCount = eventRepository.countByGroupIdAndStartsAtLessThanAndEndsAtGreaterThanEqual(
                groupId = group.id,
                to = nextMonthStart,
                from = monthStart,
            ),
            myRole = currentUserMember?.role?.toApiRole(),
            members = members.mapNotNull { member ->
                val memberUser = users[member.userId] ?: return@mapNotNull null
                GroupDetailMemberResponse(
                    userId = member.userId,
                    nickname = member.nickname,
                    role = member.role.toApiRole(),
                    color = member.color ?: DEFAULT_PROFILE_COLOR,
                )
            },
        )
    }

    companion object {
        private const val DEFAULT_PROFILE_COLOR = "RED"
    }
}
