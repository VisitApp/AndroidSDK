package com.getvisitapp.visit.healthConnect.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class BasalMetabolicRateHelperTest {

    private val zoneId = ZoneId.of("Asia/Kolkata")

    @Test
    fun dailyDataReturnsTwentyFourOrderedHourlyBuckets() = runBlocking {
        val selectedDate = LocalDate.of(2026, 9, 10)
        val startTime = selectedDate.atStartOfDay(zoneId).toInstant()
        val dataSource = FakeBasalCaloriesDataSource(
            buckets = listOf(
                BasalCaloriesBucket(
                    startTime = startTime.plus(Duration.ofHours(5)),
                    kilocalories = 75.0
                )
            ),
            total = 1_800.0
        )
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), zoneId)
        )

        val result = helper.getDailyBasalCalorieData(selectedDate)

        assertEquals(24, result.healthMetricWithDateTime?.size)
        assertEquals(selectedDate.atStartOfDay(), result.healthMetricWithDateTime?.first()?.dateTime)
        assertEquals(
            selectedDate.atStartOfDay().plusHours(23),
            result.healthMetricWithDateTime?.last()?.dateTime
        )
        assertEquals(75.0, result.healthMetricWithDateTime?.get(5)?.calorie ?: 0.0, 0.0)
        assertTrue(
            result.healthMetricWithDateTime.orEmpty()
                .filterIndexed { index, _ -> index != 5 }
                .all { it.calorie == null }
        )
        assertEquals(1_800.0, result.totalCalorie ?: 0.0, 0.0)
        assertNull(result.averageCalorie)
        assertNull(result.totalActivityTime)
        assertNull(result.averageActivityTime)
    }

    @Test
    fun currentDayQueryEndsAtCurrentTime() = runBlocking {
        val now = Instant.parse("2026-09-11T05:00:00Z")
        val selectedDate = LocalDate.of(2026, 9, 11)
        val dataSource = FakeBasalCaloriesDataSource(total = 790.0)
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(now, zoneId)
        )

        val result = helper.getDailyBasalCalorieData(selectedDate)

        assertEquals(now, dataSource.bucketRequests.single().endTime)
        assertEquals(now, dataSource.totalRequests.single().endTime)
        assertEquals(24, result.healthMetricWithDateTime?.size)
        assertTrue(result.healthMetricWithDateTime.orEmpty().all { it.calorie == null })
    }

    @Test
    fun historicalWeekUsesMondayToMondayAndAveragesSevenDays() = runBlocking {
        val selectedDate = LocalDate.of(2026, 9, 9)
        val dataSource = FakeBasalCaloriesDataSource(total = 14_000.0)
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(Instant.parse("2026-09-20T12:00:00Z"), zoneId)
        )

        val result = helper.getWeeklyBasalCalorieData(selectedDate)

        val request = dataSource.bucketRequests.single()
        assertEquals(LocalDate.of(2026, 9, 7).atStartOfDay(zoneId).toInstant(), request.startTime)
        assertEquals(LocalDate.of(2026, 9, 14).atStartOfDay(zoneId).toInstant(), request.endTime)
        assertEquals(Duration.ofDays(1), request.bucketDuration)
        assertEquals(7, result.healthMetricWithDateTime?.size)
        assertEquals(2_000.0, result.averageCalorie ?: 0.0, 0.0)
    }

    @Test
    fun currentMonthExcludesFutureTimeAndAveragesElapsedCalendarDays() = runBlocking {
        val now = Instant.parse("2026-09-11T05:00:00Z")
        val dataSource = FakeBasalCaloriesDataSource(total = 1_100.0)
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(now, zoneId)
        )

        val result = helper.getMonthlyBasalCalorieData(LocalDate.of(2026, 9, 5))

        assertEquals(now, dataSource.bucketRequests.single().endTime)
        assertEquals(30, result.healthMetricWithDateTime?.size)
        assertEquals(100.0, result.averageCalorie ?: 0.0, 0.0)
    }

    @Test
    fun absentDataKeepsBucketsEmptyAndTotalsNull() = runBlocking {
        val dataSource = FakeBasalCaloriesDataSource()
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), zoneId)
        )

        val result = helper.getWeeklyBasalCalorieData(LocalDate.of(2026, 9, 9))

        assertEquals(7, result.healthMetricWithDateTime?.size)
        assertTrue(result.healthMetricWithDateTime.orEmpty().all { it.calorie == null })
        assertNull(result.totalCalorie)
        assertNull(result.averageCalorie)
    }

    @Test
    fun futurePeriodDoesNotQueryHealthConnect() = runBlocking {
        val dataSource = FakeBasalCaloriesDataSource()
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), zoneId)
        )

        val result = helper.getDailyBasalCalorieData(LocalDate.of(2026, 9, 12))

        assertEquals(24, result.healthMetricWithDateTime?.size)
        assertTrue(dataSource.bucketRequests.isEmpty())
        assertTrue(dataSource.totalRequests.isEmpty())
        assertNull(result.totalCalorie)
    }

    @Test
    fun permissionFailureIsPropagatedToExistingErrorHandling() {
        val dataSource = FakeBasalCaloriesDataSource(failure = SecurityException("Denied"))
        val helper = BasalMetabolicRateHelper(
            dataSource = dataSource,
            clock = Clock.fixed(Instant.parse("2026-09-11T12:00:00Z"), zoneId)
        )

        assertThrows(SecurityException::class.java) {
            runBlocking {
                helper.getDailyBasalCalorieData(LocalDate.of(2026, 9, 10))
            }
        }
    }

    private data class BucketRequest(
        val startTime: Instant,
        val endTime: Instant,
        val bucketDuration: Duration
    )

    private data class TotalRequest(
        val startTime: Instant,
        val endTime: Instant
    )

    private class FakeBasalCaloriesDataSource(
        private val buckets: List<BasalCaloriesBucket> = emptyList(),
        private val total: Double? = null,
        private val failure: RuntimeException? = null
    ) : BasalCaloriesDataSource {

        val bucketRequests = mutableListOf<BucketRequest>()
        val totalRequests = mutableListOf<TotalRequest>()

        override suspend fun getBuckets(
            startTime: Instant,
            endTime: Instant,
            bucketDuration: Duration
        ): List<BasalCaloriesBucket> {
            failure?.let { throw it }
            bucketRequests += BucketRequest(startTime, endTime, bucketDuration)
            return buckets
        }

        override suspend fun getTotal(startTime: Instant, endTime: Instant): Double? {
            failure?.let { throw it }
            totalRequests += TotalRequest(startTime, endTime)
            return total
        }
    }
}
