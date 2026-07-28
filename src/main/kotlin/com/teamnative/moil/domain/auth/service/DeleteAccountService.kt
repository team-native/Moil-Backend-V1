package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.LoginSessionRepository
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.event.repository.EventRepository
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class DeleteAccountService(
    private val userAccountRepository: UserAccountRepository,
    private val loginSessionRepository: LoginSessionRepository,
    private val eventRepository: EventRepository,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun delete(user: UserAccount, email: String, password: String, leftData: Boolean) {
        if (user.email != email || !passwordEncoder.matches(password, user.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다.")
        }

        applyCalendarDataPolicy(user, leftData)
        loginSessionRepository.deleteByUserId(user.id)
        userAccountRepository.delete(user)
    }

    private fun applyCalendarDataPolicy(user: UserAccount, leftData: Boolean) {
        if (leftData) {
            return
        }

        val memberships = groupMemberRepository.findAllByUserId(user.id)
        val ownedGroupIds = memberships
            .filter { it.role == GroupRole.OWNER }
            .map { it.groupId }

        eventRepository.deleteByCreatorId(user.id)

        if (ownedGroupIds.isNotEmpty()) {
            eventRepository.deleteByGroupIdIn(ownedGroupIds)
            groupMemberRepository.deleteByGroupIdIn(ownedGroupIds)
            groupRepository.deleteAllByIdInBatch(ownedGroupIds)
        }

        groupMemberRepository.deleteByUserId(user.id)
    }
}
