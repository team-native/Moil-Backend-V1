package com.teamnative.moil.domain.availability.service

import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class AvailabilityCalculator {

    fun calculate(
        userAvailabilities: List<UserAvailability>,
    ): List<CalculatedAvailabilitySlot> {
        val normalized = userAvailabilities
            .map { it.copy(timeSlots = mergeUserSlots(it.timeSlots)) }
            .filter { it.timeSlots.isNotEmpty() }

        val boundaries = normalized
            .flatMap { it.timeSlots.flatMap { slot -> listOf(slot.startTime, slot.endTime) } }
            .distinct()
            .sorted()

        if (boundaries.size < 2) {
            return emptyList()
        }

        val calculated = boundaries
            .zipWithNext()
            .mapNotNull { (startTime, endTime) ->
                val memberIds = normalized
                    .filter { availability ->
                        availability.timeSlots.any { slot ->
                            slot.startTime <= startTime && slot.endTime >= endTime
                        }
                    }
                    .map { it.userId }
                    .sorted()

                if (memberIds.isEmpty()) {
                    null
                } else {
                    CalculatedAvailabilitySlot(
                        startTime = startTime,
                        endTime = endTime,
                        availableMemberIds = memberIds,
                    )
                }
            }

        return mergeAdjacentSlots(calculated)
    }

    private fun mergeUserSlots(slots: List<AvailabilityTimeRange>): List<AvailabilityTimeRange> {
        if (slots.isEmpty()) {
            return emptyList()
        }

        val sorted = slots
            .onEach(::validateRange)
            .sortedWith(compareBy<AvailabilityTimeRange> { it.startTime }.thenBy { it.endTime })

        return sorted.drop(1).fold(mutableListOf(sorted.first())) { merged, current ->
            val previous = merged.last()
            if (current.startTime <= previous.endTime) {
                merged[merged.lastIndex] = previous.copy(
                    endTime = maxOf(previous.endTime, current.endTime),
                )
            } else {
                merged += current
            }
            merged
        }
    }

    private fun mergeAdjacentSlots(slots: List<CalculatedAvailabilitySlot>): List<CalculatedAvailabilitySlot> {
        if (slots.isEmpty()) {
            return emptyList()
        }

        return slots.drop(1).fold(mutableListOf(slots.first())) { merged, current ->
            val previous = merged.last()
            if (previous.endTime == current.startTime &&
                previous.availableMemberIds == current.availableMemberIds
            ) {
                merged[merged.lastIndex] = previous.copy(endTime = current.endTime)
            } else {
                merged += current
            }
            merged
        }
    }

    private fun validateRange(slot: AvailabilityTimeRange) {
        require(slot.startTime < slot.endTime) {
            "가능 시간대의 시작 시간은 종료 시간보다 빨라야 합니다."
        }
    }
}

data class UserAvailability(
    val userId: Long,
    val timeSlots: List<AvailabilityTimeRange>,
)

data class AvailabilityTimeRange(
    val startTime: LocalTime,
    val endTime: LocalTime,
)

data class CalculatedAvailabilitySlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val availableMemberIds: List<Long>,
) {
    val availableCount: Int
        get() = availableMemberIds.size
}
