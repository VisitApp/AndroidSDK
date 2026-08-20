package com.getvisitapp.visit.healthConnect.learningMaterial

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.visit.healthConnect.TimeUtil.convertToLocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class Tutorials(val healthConnectClient: HealthConnectClient) {

    fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            val startTime =
                LocalDateTime.of(LocalDate.parse("2024-08-25"), LocalTime.MIN)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            val endTime =
                LocalDateTime.of(LocalDate.parse("2024-08-25"), LocalTime.MAX)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

            aggregateSteps(healthConnectClient, startTime, endTime)
        }
    }

    suspend fun readStepsByTimeRange(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            response.records.forEach { stepRecord ->
                Timber.d(
                    "stepRecord: count: ${stepRecord.count}, " +
                        "startTime: ${stepRecord.startTime} (${stepRecord.startTime.toEpochMilli()}), " +
                        "endTime: ${stepRecord.endTime} (${stepRecord.endTime.toEpochMilli()}), " +
                        "startTime (Local): ${stepRecord.startTime.convertToLocalDateTime()}, " +
                        "endTime (Local): ${stepRecord.endTime.convertToLocalDateTime()}, " +
                        "dataOrigin: ${stepRecord.metadata.dataOrigin.packageName}"
                )
            }
        } catch (exception: Exception) {
            Timber.d("error: ${exception.message}")
        }
    }

    private suspend fun aggregateSteps(
        healthConnectClient: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ) {
        try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            Timber.d(
                "stepCount: ${response[StepsRecord.COUNT_TOTAL]}, dataOrigins: ${
                    response.dataOrigins.joinToString(separator = ",") { it.packageName }
                }"
            )
        } catch (exception: Exception) {
            Timber.d("error: ${exception.message}")
        }
    }
}
