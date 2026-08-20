package com.getvisitapp.visit.healthConnect.helper

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.visit.healthConnect.TimeUtil.convertLocalDateTimeToEpochMillis
import com.getvisitapp.visit.healthConnect.TimeUtil.convertToLocalDateTime
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit


class DailySyncManager(private val healthConnectClient: HealthConnectClient) {

    suspend fun getDailySyncData(
        dailyLastSyncTimeStamp: Long,
        dataBeyond30DaysIsAllowed: Boolean = false
    ): List<DailySyncHealthMetric> {

        //Case 1: If the timestamp is 0, then take last 30day timestamp and sync it from there.
        //Case 2: If the timestamp is older then 30 days, then only sync the data for last 30 days.
        //Case 3: else sync from the dailyLastSyncTimeStamp


        //Case 1:
        val normalizedDateTime: LocalDateTime = if (dailyLastSyncTimeStamp == 0L) {
            LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)
        } else {
            Instant.ofEpochMilli(dailyLastSyncTimeStamp).convertToLocalDateTime().withHour(0)
                .withMinute(0).withSecond(0).withNano(0)
        }


        var startDate: Instant =
            LocalDateTime.of(normalizedDateTime.toLocalDate().minusDays(1), LocalTime.MIN)
                .atZone(ZoneId.systemDefault()).toInstant()


        val endDate: Instant =
            LocalDateTime.of(LocalDate.now(), LocalTime.MAX).atZone(ZoneId.systemDefault())
                .toInstant()

        var daysInBetween = ChronoUnit.DAYS.between(startDate, endDate)
            .toInt() + 1 // "+1" because current days is not included

        Timber.tag("mytag")
            .d("startDate: $startDate, endDate: $endDate, normalizedDateTime: $normalizedDateTime, daysBetween: $daysInBetween")


        //Case 2:
        if (!dataBeyond30DaysIsAllowed) {
            if (daysInBetween > 30) {
                startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(30)
                    .atZone(ZoneId.systemDefault()).toInstant()

                daysInBetween = ChronoUnit.DAYS.between(startDate, endDate)
                    .toInt() + 1 // "+1" because current days is not included
            }
        }


        Timber.tag("mytag")
            .d("After normalization: startTime: $startDate, endTime: $endDate, normalizedDateTime: $normalizedDateTime, daysBetween: $daysInBetween, dataBeyond30DaysIsAllowed: $dataBeyond30DaysIsAllowed")


        val dailyHealthMetric: List<DailySyncHealthMetric> = aggregateHealthMetricBasedOnDuration(
            startDateInstant = startDate,
            endDateInstant = endDate,
            daysInBetween = daysInBetween,
            startDateTime = startDate.convertToLocalDateTime(),
            endDateTime = endDate.convertToLocalDateTime()
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
                    StepsRecord.COUNT_TOTAL
                ),
                timeRangeFilter = TimeRangeFilter.between(startDateInstant, endDateInstant),
                timeRangeSlicer = Duration.ofDays(1)
            )
        )

        response.forEach { result: AggregationResultGroupedByDuration ->


            val bucketStartDateTime: LocalDateTime =
                LocalDateTime.ofInstant(result.startTime, ZoneId.systemDefault())

            val bucketEndDateTime = LocalDateTime.ofInstant(result.endTime, ZoneId.systemDefault())


            val isPresent =
                finalHealthMetricDataList.contains(DailySyncHealthMetric(dateTime = bucketStartDateTime))

            if (isPresent) {

                val index = finalHealthMetricDataList.indexOf(
                    DailySyncHealthMetric(
                        dateTime = bucketStartDateTime
                    )
                )

                finalHealthMetricDataList[index].steps = result.result[StepsRecord.COUNT_TOTAL] ?: 0
            }

            Timber.tag("mytag").d(
                "bucketStartDateTime: %s, bucketEndDateTime: %s, steps total: %s, startTime: %s, endTime: %s",
                bucketStartDateTime,
                bucketEndDateTime,
                result.result[StepsRecord.COUNT_TOTAL],
                result.startTime,
                result.endTime
            )

        }


        return finalHealthMetricDataList
    }
}
