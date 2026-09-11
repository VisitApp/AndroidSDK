package com.getvisitapp.visit.healthConnect.data

import com.getvisitapp.visit.healthConnect.model.internal.HealthMetricData
import com.getvisitapp.visit.healthConnect.model.internal.HealthMetricsWithDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BasalCalorieGraphFormatterTest {

    @Test
    fun dailyDataUsesTwentyFourLabelsWholeKilocaloriesAndZeroForMissingValues() {
        val healthMetricData = createHealthMetricData(
            bucketCount = 24,
            caloriesByIndex = mapOf(0 to 42.9, 2 to 17.1)
        )

        val result = formatBasalCalorieGraphData(healthMetricData, frequency = "day")

        assertEquals(
            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[42,0,17,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0], 'basalCalories', 'day','0')",
            result
        )
    }

    @Test
    fun weeklyDataUsesSevenLabelsAndBasalCaloriesType() {
        val healthMetricData = createHealthMetricData(
            bucketCount = 7,
            caloriesByIndex = mapOf(0 to 1_500.0, 6 to 1_700.0)
        )

        val result = formatBasalCalorieGraphData(healthMetricData, frequency = "week")

        assertEquals(
            "DetailedGraph.updateData([1,2,3,4,5,6,7],[1500,0,0,0,0,0,1700], 'basalCalories', 'week','0')",
            result
        )
    }

    @Test
    fun monthlyDataUsesExactCalendarDayCount() {
        listOf(28, 29, 30, 31).forEach { bucketCount ->
            val healthMetricData = createHealthMetricData(bucketCount = bucketCount)
            val labels = (1..bucketCount).joinToString(separator = ",")
            val values = List(bucketCount) { "0" }.joinToString(separator = ",")

            val result = formatBasalCalorieGraphData(healthMetricData, frequency = "month")

            assertEquals(
                "DetailedGraph.updateData([$labels],[$values], 'basalCalories', 'month','0')",
                result
            )
        }
    }

    private fun createHealthMetricData(
        bucketCount: Int,
        caloriesByIndex: Map<Int, Double> = emptyMap()
    ): HealthMetricData {
        val startDate = LocalDate.of(2026, 1, 1).atStartOfDay()
        val entries = List(bucketCount) { index ->
            HealthMetricsWithDateTime(
                calorie = caloriesByIndex[index],
                dateTime = startDate.plusDays(index.toLong())
            )
        }

        return HealthMetricData(healthMetricWithDateTime = entries)
    }
}
