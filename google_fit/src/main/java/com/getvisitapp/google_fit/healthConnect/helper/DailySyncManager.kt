package com.getvisitapp.google_fit.healthConnect.helper

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertLocalDateTimeToEpochMillis
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertToLocalDateTime
import com.getvisitapp.google_fit.healthConnect.data.SleepHelper
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit


class DailySyncManager(private val healthConnectClient: HealthConnectClient) {

    private val activityTimeHelper = ActivityTimeHelper()
    private val sleepHelper = SleepHelper(healthConnectClient)


    suspend fun getDailySyncData(dailyLastSyncTimeStamp: Long): List<DailySyncHealthMetric> {

        val normalizedDateTime: LocalDateTime =
            Instant.ofEpochMilli(dailyLastSyncTimeStamp).convertToLocalDateTime().withHour(0)
                .withMinute(0).withSecond(0).withNano(0)



        val startTime: Instant =
            LocalDateTime.of(normalizedDateTime.toLocalDate().minusDays(1), LocalTime.MIN)
                .atZone(ZoneId.systemDefault()).toInstant()


        val endTime: Instant =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()

        val daysBetween = ChronoUnit.DAYS.between(startTime, endTime)
            .toInt() + 1 // "+1" because current days is not included

        Timber.d("startTime: $startTime, endTime: $endTime")


        Timber.d("normalizedDateTime: $normalizedDateTime, daysBetween: $daysBetween")


        val dailyHealthMetric: List<DailySyncHealthMetric> = aggregateHealthMetricBasedOnDuration(
            startDateInstant = startTime,
            endDateInstant = endTime,
            daysInBetween = daysBetween,
            startDateTime = startTime.convertToLocalDateTime(),
            endDateTime = endTime.convertToLocalDateTime()
        )

        return dailyHealthMetric


    }

    private suspend fun aggregateHealthMetricBasedOnDuration(
        startDateInstant: Instant,
        endDateInstant: Instant,
        daysInBetween: Int,
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime
    ): List<DailySyncHealthMetric> {

        val finalHealthMetricDataList = mutableListOf<DailySyncHealthMetric>()

        Timber.d("startDateTime: $startDateTime, endDateTime : $endDateTime")

        for (i in 0..<daysInBetween.toLong()) {
            finalHealthMetricDataList.add(
                DailySyncHealthMetric(
                    dateTime = startDateTime.plusDays(i),
                    date = startDateTime.plusDays(i).convertLocalDateTimeToEpochMillis()
                )
            )
        }

        Timber.d("finalHealthMetricDataList: $finalHealthMetricDataList")

        val response = healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL,
                    DistanceRecord.DISTANCE_TOTAL,
                    ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                    TotalCaloriesBurnedRecord.ENERGY_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofDays(1)
            )
        )

        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime = LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())


            val sleepMetric = sleepHelper.getDailySleepData(bucketStartDateTime.toLocalDate())

            val isPresent =
                finalHealthMetricDataList.contains(DailySyncHealthMetric(dateTime = bucketStartDateTime))

            if (isPresent) {

                val index = finalHealthMetricDataList.indexOf(
                    DailySyncHealthMetric(
                        dateTime = bucketStartDateTime
                    )
                )

                finalHealthMetricDataList[index].steps = result.result[StepsRecord.COUNT_TOTAL] ?: 0

                finalHealthMetricDataList[index].calorie =
                    result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toLong()
                        ?: 0L

                finalHealthMetricDataList[index].distance =
                    result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.toLong() ?: 0

                finalHealthMetricDataList[index].activity =
                    activityTimeHelper.getTotalActivityTimeForDay(
                        healthConnectClient,
                        result.startTime,
                        result.endTime
                    ).toSeconds()

                finalHealthMetricDataList[index].sleep =
                    "${sleepMetric.sleepStartTimeMillis}-${sleepMetric.sleepEndTimeMillis}"

            }

            Timber.d(
                "bucketStartDateTime: $bucketStartDateTime ," + "bucketEndDateTime: $bucketEndDateTime, " + "steps total: ${result.result[StepsRecord.COUNT_TOTAL]} " + "distance total: ${result.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters} " + "exercise total: ${result.result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()} " + "calorie total: ${result.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories} " + "sleep total: ${result.result[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMinutes()} " + "startTime:${result.startTime} ," + "endTime: ${result.endTime}," + "sleepMetric: $sleepMetric"
            )

        }


        return finalHealthMetricDataList
    }
}