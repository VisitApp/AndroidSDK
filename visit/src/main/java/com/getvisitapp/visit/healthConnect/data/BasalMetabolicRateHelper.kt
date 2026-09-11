package com.getvisitapp.visit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.visit.healthConnect.model.internal.HealthMetricData
import com.getvisitapp.visit.healthConnect.model.internal.HealthMetricsWithDateTime
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal data class BasalCaloriesBucket(
    val startTime: Instant,
    val kilocalories: Double?
)

internal interface BasalCaloriesDataSource {
    suspend fun getBuckets(
        startTime: Instant,
        endTime: Instant,
        bucketDuration: Duration
    ): List<BasalCaloriesBucket>

    suspend fun getTotal(startTime: Instant, endTime: Instant): Double?
}

private class HealthConnectBasalCaloriesDataSource(
    private val healthConnectClient: HealthConnectClient
) : BasalCaloriesDataSource {

    override suspend fun getBuckets(
        startTime: Instant,
        endTime: Instant,
        bucketDuration: Duration
    ): List<BasalCaloriesBucket> {
        return healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                timeRangeSlicer = bucketDuration
            )
        ).map { result ->
            BasalCaloriesBucket(
                startTime = result.startTime,
                kilocalories = result.result[
                    BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL
                ]?.inKilocalories
            )
        }
    }

    override suspend fun getTotal(startTime: Instant, endTime: Instant): Double? {
        val result = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )

        return result[BasalMetabolicRateRecord.BASAL_CALORIES_TOTAL]?.inKilocalories
    }
}

class BasalMetabolicRateHelper internal constructor(
    private val dataSource: BasalCaloriesDataSource,
    private val clock: Clock
) {

    constructor(healthConnectClient: HealthConnectClient) : this(
        dataSource = HealthConnectBasalCaloriesDataSource(healthConnectClient),
        clock = Clock.systemDefaultZone()
    )

    suspend fun getDailyBasalCalorieData(selectedDate: LocalDate): HealthMetricData {
        return getBasalCalorieData(
            periodStartDate = selectedDate,
            periodEndDateExclusive = selectedDate.plusDays(1),
            bucketCount = 24,
            bucketDuration = Duration.ofHours(1),
            bucketDateTime = { index -> selectedDate.atStartOfDay().plusHours(index.toLong()) },
            includeAverage = false
        )
    }

    suspend fun getWeeklyBasalCalorieData(selectedDate: LocalDate): HealthMetricData {
        val weekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        return getBasalCalorieData(
            periodStartDate = weekStart,
            periodEndDateExclusive = weekStart.plusDays(DAYS_IN_WEEK.toLong()),
            bucketCount = DAYS_IN_WEEK,
            bucketDuration = Duration.ofDays(1),
            bucketDateTime = { index -> weekStart.atStartOfDay().plusDays(index.toLong()) },
            includeAverage = true
        )
    }

    suspend fun getMonthlyBasalCalorieData(selectedDate: LocalDate): HealthMetricData {
        val monthStart = selectedDate.withDayOfMonth(1)

        return getBasalCalorieData(
            periodStartDate = monthStart,
            periodEndDateExclusive = monthStart.plusMonths(1),
            bucketCount = monthStart.lengthOfMonth(),
            bucketDuration = Duration.ofDays(1),
            bucketDateTime = { index -> monthStart.atStartOfDay().plusDays(index.toLong()) },
            includeAverage = true
        )
    }

    private suspend fun getBasalCalorieData(
        periodStartDate: LocalDate,
        periodEndDateExclusive: LocalDate,
        bucketCount: Int,
        bucketDuration: Duration,
        bucketDateTime: (Int) -> LocalDateTime,
        includeAverage: Boolean
    ): HealthMetricData {
        val entries = List(bucketCount) { index ->
            HealthMetricsWithDateTime(dateTime = bucketDateTime(index))
        }
        val result = HealthMetricData(healthMetricWithDateTime = entries)
        val zoneId = clock.zone
        val periodStart = periodStartDate.atStartOfDay(zoneId).toInstant()
        val periodEnd = periodEndDateExclusive.atStartOfDay(zoneId).toInstant()
        val queryEnd = minOf(periodEnd, clock.instant())

        if (!queryEnd.isAfter(periodStart)) {
            return result
        }

        val entriesByDateTime = entries.associateBy { it.dateTime }
        dataSource.getBuckets(periodStart, queryEnd, bucketDuration).forEach { bucket ->
            val dateTime = LocalDateTime.ofInstant(bucket.startTime, zoneId)
            entriesByDateTime[dateTime]?.calorie = bucket.kilocalories
        }

        val totalBasalCalories = dataSource.getTotal(periodStart, queryEnd)
        result.totalCalorie = totalBasalCalories

        if (includeAverage && totalBasalCalories != null) {
            result.averageCalorie = totalBasalCalories / elapsedDayCount(
                periodStartDate = periodStartDate,
                periodEndDateExclusive = periodEndDateExclusive,
                queryEnd = queryEnd
            )
        }

        return result
    }

    private fun elapsedDayCount(
        periodStartDate: LocalDate,
        periodEndDateExclusive: LocalDate,
        queryEnd: Instant
    ): Long {
        val dayAfterLastElapsedDay = LocalDateTime.ofInstant(
            queryEnd.minusNanos(1),
            clock.zone
        ).toLocalDate().plusDays(1)
        val elapsedEndDateExclusive = minOf(periodEndDateExclusive, dayAfterLastElapsedDay)

        return ChronoUnit.DAYS.between(periodStartDate, elapsedEndDateExclusive)
            .coerceAtLeast(1)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
