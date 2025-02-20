package com.getvisitapp.google_fit.healthConnect.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getvisitapp.google_fit.healthConnect.TimeUtil.convertEpochMillisToLocalDateTime
import com.getvisitapp.google_fit.healthConnect.model.internal.HealthMetricData
import com.getvisitapp.google_fit.healthConnect.model.internal.SleepModel
import com.getvisitapp.google_fit.healthConnect.model.internal.StepsAndSleep
import com.google.gson.Gson
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
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL
                ), timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )


        // The result may be null if no data is available in the time range
        val stepCount: Long? = stepsResponse[StepsRecord.COUNT_TOTAL]


        val stepsAndSleep = StepsAndSleep(stepCount)

        Timber.d(
            "Steps: ${stepsAndSleep.steps}"
        )

        val finalString =
            "window.updateFitnessPermissions(true,${stepsAndSleep.steps ?: 0},0)"

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
                metrics = setOf(
                    StepsRecord.COUNT_TOTAL
                ), timeRangeFilter = TimeRangeFilter.between(stepsStartTime, stepsEndTime)
            )
        )


        // The result may be null if no data is available in the time range
        val stepCount: Long = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

        Timber.d(
            "Steps: $stepCount"
        )

        return stepCount
    }

    /**
     * Steps Reading Functions
     */

//  1.
//  Expected format: DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[0, 0, 0, 0, 0, 0, 0, 43, 462, 0, 104, 269, 188, 0, 10, 0, 0, 513, 63, 0, 11, 440, 0, 0], 'steps', 'day','57')

    suspend fun getDailyStepsData(timeStamp: Long): String {
        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()


        val healthMetricData: HealthMetricData =
            stepsHelper.getDailyStepsData(selectedDate = requestedTimeStamp.toLocalDate())

        Timber.d("getDailyStepsData: ${healthMetricData.healthMetricWithDateTime?.size}")

//        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, hourlyStepsWithDateTime ->
//            Timber.d("index: $index, steps: ${hourlyStepsWithDateTime.steps}, hour:${hourlyStepsWithDateTime.dateTime}")
//        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}")

        val duration = healthMetricData.totalActivityTime
//        duration?.let {
//            Timber.d("duration: ${duration.toHoursPart()}::${duration.toMinutesPart()}::${duration.toSecondsPart()}")
//        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[$stepsSeparated], 'steps', 'day','${duration?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

    //2.
    //Expected Output: DetailedGraph.updateData([1,2,3,4,5,6,7],[88, 0, 0, 0, 0, 0, 0],'steps', 'week','1')

    suspend fun getWeeklyStepsData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = stepsHelper.getWeeklyStepsData(
            selectedDate = requestedTimeStamp.toLocalDate()
        )

        Timber.d("getWeeklyStepsData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, stepsOfEachDateOfTheWeek ->
            Timber.d("index: $index, steps: ${stepsOfEachDateOfTheWeek.steps}, hour:${stepsOfEachDateOfTheWeek.dateTime}")
        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}, averageSteps:${healthMetricData.averageSteps}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHoursPart()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHoursPart()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[$stepsSeparated], 'steps', 'week','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

//  3.
//  Expected Output : DetailedGraph . updateData ([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31], [8728, 1220, 5633, 2231, 696, 2482, 6081, 8087, 3207, 9807, 1690, 2917, 1267, 4786, 1193, 1871, 6603, 2154, 2182, 1029, 2445, 3183, 549, 3458, 2103, 88, 0, 0, 0, 0, 0], 'steps', 'month', '36')

    suspend fun getMonthlyStepsData(timeStamp: Long): String {

        val requestedTimeStamp: LocalDateTime = timeStamp.convertEpochMillisToLocalDateTime()

        val healthMetricData: HealthMetricData = stepsHelper.getMonthlyStepsData(
            requestedTimeStamp.toLocalDate()
        )

        Timber.d("stepsGraphData: $healthMetricData")
//
        healthMetricData.healthMetricWithDateTime?.forEachIndexed { index, stepsOfEachDateOfTheMonth ->
            Timber.d("index: $index, steps: ${stepsOfEachDateOfTheMonth.steps}, hour:${stepsOfEachDateOfTheMonth.dateTime}")
        }

        Timber.d("totalSteps: ${healthMetricData.totalSteps}, averageSteps:${healthMetricData.averageSteps}")

        val totalActivityTime = healthMetricData.totalActivityTime
        totalActivityTime?.let {
            Timber.d("totalActivityTime: ${totalActivityTime.toHours()}::${totalActivityTime.toMinutesPart()}::${totalActivityTime.toSecondsPart()}")
        }

        val averageActivityTime = healthMetricData.averageActivityTime
        averageActivityTime?.let {
            Timber.d("averageActivityTime: ${averageActivityTime.toHours()}::${averageActivityTime.toMinutesPart()}::${averageActivityTime.toSecondsPart()}")
        }


        //Formatting the string before sending it to webapp.
        val stepsSeparated = healthMetricData.healthMetricWithDateTime?.map { it.steps }
            ?.joinToString(separator = ",") { step -> if (step == null) "0" else "$step" }

        val webString =
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[$stepsSeparated], 'steps', 'month','${averageActivityTime?.toMinutes()}')"


        Timber.d("value: $webString")

        return webString
    }

}