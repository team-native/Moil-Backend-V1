package com.teamnative.moil.domain.notification

import com.teamnative.moil.domain.event.service.AttendanceUpdatedNotification
import com.teamnative.moil.domain.event.service.EventCreatedNotification
import com.teamnative.moil.domain.event.service.EventUpdatedNotification
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EventNotificationListener(
    private val pushNotificationService: PushNotificationService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onCreated(event: EventCreatedNotification) {
        pushNotificationService.sendToGroup(
            event.actorUserId,
            event.groupId,
            ApnsPayload("EVENT_CREATED", event.groupId, event.eventId, "새 일정이 등록되었습니다.", event.title),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onUpdated(event: EventUpdatedNotification) {
        pushNotificationService.sendToEventAttendeesOrGroup(
            event.actorUserId,
            event.groupId,
            event.eventId,
            ApnsPayload("EVENT_UPDATED", event.groupId, event.eventId, "일정이 수정되었습니다.", event.title),
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAttendanceUpdated(event: AttendanceUpdatedNotification) {
        val statusText = if (event.status.name == "ATTENDING") "참석" else "불참"
        pushNotificationService.sendToUser(
            event.actorUserId,
            event.creatorUserId,
            ApnsPayload("EVENT_ATTENDANCE_UPDATED", event.groupId, event.eventId, "참석 여부가 변경되었습니다.", "${event.title} 일정에 $statusText 응답이 등록되었습니다."),
        )
    }
}
