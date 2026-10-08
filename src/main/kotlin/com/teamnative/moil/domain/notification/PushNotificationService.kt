package com.teamnative.moil.domain.notification

import com.teamnative.moil.domain.auth.repository.UserDeviceTokenRepository
import com.teamnative.moil.domain.auth.model.DevicePlatform
import com.teamnative.moil.domain.event.model.EventAttendanceStatus
import com.teamnative.moil.domain.event.repository.EventAttendanceRepository
import com.teamnative.moil.domain.event.repository.EventSharedMemberRepository
import com.teamnative.moil.domain.group.repository.GroupMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PushNotificationService(
    private val groupMemberRepository: GroupMemberRepository,
    private val eventSharedMemberRepository: EventSharedMemberRepository,
    private val eventAttendanceRepository: EventAttendanceRepository,
    private val userDeviceTokenRepository: UserDeviceTokenRepository,
    private val apnsClient: ApnsClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendToGroup(actorUserId: Long, groupId: Long, payload: ApnsPayload) {
        val recipients = groupMemberRepository.findAllByGroupId(groupId)
            .filter { it.notificationEnabled && it.userId != actorUserId }
            .map { it.userId }
        sendToUsers(recipients, payload)
    }

    fun sendToEventAttendeesOrGroup(actorUserId: Long, groupId: Long, eventId: Long, payload: ApnsPayload) {
        val activeTargetIds = eventSharedMemberRepository.findAllByEventId(eventId)
            .map { it.userId }
            .toSet()
        val attendees = eventAttendanceRepository.findAllByEventId(eventId)
            .filter { it.status == EventAttendanceStatus.ATTENDING && it.userId in activeTargetIds }
            .map { it.userId }
            .toSet()
        if (attendees.isEmpty()) sendToGroup(actorUserId, groupId, payload)
        else sendToUsers(attendees.filter { it != actorUserId }, payload)
    }

    fun sendToUser(actorUserId: Long, userId: Long, payload: ApnsPayload) {
        if (actorUserId == userId) return
        val enabled = groupMemberRepository.findAllByGroupId(payload.groupId)
            .any { it.userId == userId && it.notificationEnabled }
        if (enabled) sendToUsers(listOf(userId), payload)
    }

    private fun sendToUsers(userIds: Collection<Long>, payload: ApnsPayload) {
        if (userIds.isEmpty()) return
        val tokens = userDeviceTokenRepository.findAllByUserIdIn(userIds)
            .filter { it.platform == DevicePlatform.IOS }
            .distinctBy { it.token }
        tokens.forEach { token ->
            val result = apnsClient.send(token.token, payload)
            if (result.invalidToken) {
                userDeviceTokenRepository.deleteByUserIdAndToken(token.userId, token.token)
                logger.info("Removed invalid APNs device token for user {}.", token.userId)
            }
        }
    }
}
