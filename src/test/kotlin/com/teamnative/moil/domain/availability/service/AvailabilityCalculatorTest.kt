package com.teamnative.moil.domain.availability.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalTime

class AvailabilityCalculatorTest {

    private val calculator = AvailabilityCalculator()

    @Test
    fun `overlapping ranges are split and member counts are calculated`() {
        val result = calculator.calculate(
            listOf(
                availability(10, "12:31", "13:42"),
                availability(11, "13:10", "14:05"),
                availability(12, "13:00", "13:30"),
            ),
        )

        assertEquals(
            listOf(
                slot("12:31", "13:00", listOf(10L)),
                slot("13:00", "13:10", listOf(10L, 12L)),
                slot("13:10", "13:30", listOf(10L, 11L, 12L)),
                slot("13:30", "13:42", listOf(10L, 11L)),
                slot("13:42", "14:05", listOf(11L)),
            ),
            result,
        )
    }

    @Test
    fun `overlapping ranges of the same user are counted once`() {
        val result = calculator.calculate(
            listOf(
                UserAvailability(
                    userId = 10,
                    timeSlots = listOf(
                        range("12:00", "13:00"),
                        range("12:30", "14:00"),
                    ),
                ),
                availability(11, "13:30", "14:30"),
            ),
        )

        assertEquals(
            listOf(
                slot("12:00", "13:30", listOf(10L)),
                slot("13:30", "14:00", listOf(10L, 11L)),
                slot("14:00", "14:30", listOf(11L)),
            ),
            result,
        )
    }

    @Test
    fun `invalid range is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            calculator.calculate(listOf(availability(10, "14:00", "13:00")))
        }
    }

    private fun availability(userId: Long, start: String, end: String) =
        UserAvailability(userId, listOf(range(start, end)))

    private fun range(start: String, end: String) =
        AvailabilityTimeRange(LocalTime.parse(start), LocalTime.parse(end))

    private fun slot(start: String, end: String, members: List<Long>) =
        CalculatedAvailabilitySlot(LocalTime.parse(start), LocalTime.parse(end), members)
}
