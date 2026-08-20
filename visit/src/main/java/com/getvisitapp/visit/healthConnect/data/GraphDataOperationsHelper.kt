package com.getvisitapp.visit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.visit.healthConnect.TimeUtil.convertEpochMillisToLocalDateTime
import com.getvisitapp.visit.healthConnect.model.internal.HealthMetricData
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GraphDataOperationsHelper(healthConnectClient: HealthConnectClient) {

    val stepsHelper by lazy { StepHelper(healthConnectClient) }

    suspend fun getTodayStepsAndSleepData(
        healthConnectClient: HealthConnectClient
    ): String {
        val stepsStartTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).atZone(ZoneId.systemDefault())
                .toInstant()
        val stepsEndTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()
        val stepsResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )
        val stepCount: Long? = stepsResponse[StepsRecord.COUNT_TOTAL]
        val finalString = "window.updateFitnessPermissions(true,${stepCount ?: 0},0)"

        Timber.d(finalString)
        return finalString
    }

    suspend fun getTodaySteps(
        healthConnectClient: HealthConnectClient
    ): Long {
        val stepsStartTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).atZone(ZoneId.systemDefault())
                .toInstant()
        val stepsEndTime =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()
        val stepsResponse = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )
        val stepCount: Long = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

        Timber.d("Steps: $stepCount")
        return stepCount
    }

    suspend fun getTodaySleepMinutes(): Long = 0

    suspend fun getTodayCalorieCount(
        @Suppress("UNUSED_PARAMETER") healthConnectClient: HealthConnectClient
    ): Long = 0

    suspend fun getDailyStepsData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()
        val healthMetricData: HealthMetricData =
            stepsHelper.getDailyStepsData(selectedDate = requestedTimeStamp.toLocalDate())
        val duration = healthMetricData.totalActivityTime
        val stepsSeparated = healthMetricData.healthMetricWithDateTime
            ?.joinToString(separator = ",") { metric -> "${metric.steps ?: 0}" }
        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[$stepsSeparated], 'steps', 'day','${duration?.toMinutes()}')"

        Timber.d("value: $webString")
        return webString
    }

    suspend fun getWeeklyStepsData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()
        val healthMetricData: HealthMetricData =
            stepsHelper.getWeeklyStepsData(selectedDate = requestedTimeStamp.toLocalDate())
        val averageActivityTime = healthMetricData.averageActivityTime
        val stepsSeparated = healthMetricData.healthMetricWithDateTime
            ?.joinToString(separator = ",") { metric -> "${metric.steps ?: 0}" }
        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[$stepsSeparated], 'steps', 'week','${averageActivityTime?.toMinutes()}')"

        Timber.d("value: $webString")
        return webString
    }

    suspend fun getMonthlyStepsData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()
        val healthMetricData: HealthMetricData =
            stepsHelper.getMonthlyStepsData(requestedTimeStamp.toLocalDate())
        val averageActivityTime = healthMetricData.averageActivityTime
        val stepsSeparated = healthMetricData.healthMetricWithDateTime
            ?.joinToString(separator = ",") { metric -> "${metric.steps ?: 0}" }
        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[$stepsSeparated], 'steps', 'month','${averageActivityTime?.toMinutes()}')"

        Timber.d("value: $webString")
        return webString
    }

    suspend fun getDailyDistanceData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "distance", frequency = "day", count = 24)

    suspend fun getWeeklyDistanceData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "distance", frequency = "week", count = 7)

    suspend fun getMonthlyDistanceData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "distance", frequency = "month", count = 31)

    suspend fun getDailyCalorieData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "calories", frequency = "day", count = 24)

    suspend fun getWeeklyCalorieData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "calories", frequency = "week", count = 7)

    suspend fun getMonthlyCalorieData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = zeroGraphData(type = "calories", frequency = "month", count = 31)

    suspend fun getDailySleepData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String = "DetailedGraph.updateDailySleep(0,0)"

    suspend fun getWeeklySleepData(
        @Suppress("UNUSED_PARAMETER") timeStamp: Long
    ): String {
        val weeklySleepJson = WEEK_DAYS.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { day ->
            """{"day":"$day","sleepTime":0,"startTimestamp":0,"wakeupTime":0}"""
        }

        return "DetailedGraph.updateSleepData(JSON.stringify($weeklySleepJson));"
    }

    private fun zeroGraphData(type: String, frequency: String, count: Int): String {
        val labels = (1..count).joinToString(separator = ",")
        val values = List(count) { 0 }.joinToString(separator = ",")
        val webString =
            "DetailedGraph.updateData([$labels],[$values], '$type', '$frequency','0')"

        Timber.d("value: $webString")
        return webString
    }

    private companion object {
        val WEEK_DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
}
