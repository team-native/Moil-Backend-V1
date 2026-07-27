package com.teamnative.moil.domain.group.service

import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.group.model.Group
import com.teamnative.moil.domain.group.model.GroupMember
import com.teamnative.moil.domain.group.model.GroupRole
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import com.teamnative.moil.domain.group.repository.GroupRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

@SpringBootTest
class GroupMemberIntegrityServiceTest {

    @Autowired
    private lateinit var groupMemberIntegrityService: GroupMemberIntegrityService

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userAccountRepository: UserAccountRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `sync owner group ids fixes only mismatched rows`() {
        val group = createGroup()
        val owner = createUser()
        val member = createUser()
        val ownerMember = groupMemberRepository.saveAndFlush(
            GroupMember(
                groupId = group.id,
                userId = owner.id,
                role = GroupRole.OWNER,
                joinedAt = Instant.now(),
            ),
        )
        val normalMember = groupMemberRepository.saveAndFlush(
            GroupMember(
                groupId = group.id,
                userId = member.id,
                role = GroupRole.MEMBER,
                joinedAt = Instant.now(),
            ),
        )

        jdbcTemplate.update(
            "update group_members set owner_group_id = null where id = ?",
            ownerMember.id,
        )
        jdbcTemplate.update(
            "update group_members set owner_group_id = ? where id = ?",
            group.id,
            normalMember.id,
        )
        entityManager.clear()

        groupMemberIntegrityService.syncOwnerGroupIds()
        entityManager.clear()

        val syncedOwner = groupMemberRepository.findById(ownerMember.id).orElseThrow()
        val syncedMember = groupMemberRepository.findById(normalMember.id).orElseThrow()

        assert(syncedOwner.role == GroupRole.OWNER)
        assert(syncedOwner.ownerGroupId == group.id)
        assert(syncedMember.role == GroupRole.MEMBER)
        assert(syncedMember.ownerGroupId == null)
    }

    private fun createGroup(): Group =
        groupRepository.save(
            Group(
                name = "스터디 그룹",
                inviteCode = "invite_${UUID.randomUUID().toString().take(8)}",
                createdAt = Instant.now(),
            ),
        )

    private fun createUser(): UserAccount =
        userAccountRepository.save(
            UserAccount(
                name = "test-user",
                email = "test-${UUID.randomUUID()}@example.com",
                passwordHash = "password",
            ),
        )
}
