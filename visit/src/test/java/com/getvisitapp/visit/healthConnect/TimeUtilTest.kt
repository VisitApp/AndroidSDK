package com.getvisitapp.visit.healthConnect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class TimeUtilTest {

    @Test
    fun getTodayElapsedTimeRangeInstantUsesLocalMidnightToCurrentMorningTime() {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = Instant.parse("2026-07-24T03:30:00Z")
        val clock = Clock.fixed(now, zoneId)

        val (startTime, endTime) = TimeUtil.getTodayElapsedTimeRangeInstant(clock)

        val expectedStartTime =
            LocalDateTime.of(LocalDate.of(2026, 7, 24), LocalTime.MIN)
                .atZone(zoneId)
                .toInstant()

        assertEquals(expectedStartTime, startTime)
        assertEquals(now, endTime)
    }

    @Test
    fun getTodayElapsedTimeRangeInstantUsesCurrentLateEveningTimeInsteadOfEndOfDay() {
        val zoneId = ZoneId.of("Asia/Kolkata")
        val now = Instant.parse("2026-07-24T17:45:30Z")
        val clock = Clock.fixed(now, zoneId)

        val (_, endTime) = TimeUtil.getTodayElapsedTimeRangeInstant(clock)

        val endOfDay =
            LocalDateTime.of(LocalDate.of(2026, 7, 24), LocalTime.MAX)
                .atZone(zoneId)
                .toInstant()

        assertEquals(now, endTime)
        assertNotEquals(endOfDay, endTime)
    }
}
