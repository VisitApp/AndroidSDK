package com.getvisitapp.google_fit.healthConnect.model

import java.time.Duration

data class SleepMetric(
    val sleepDuration: Duration,
    val formattedSleepDuration: String,
    val sleepStartTimeMillis: Long,
    val sleepEndTimeMillis: Long,
    val sleepDurationInMillis: Long,
) {
    override fun toString(): String {
        return "SleepMetric( sleepDuration: ${sleepDuration.toMinutes()}, " +
                "formattedSleepDuration: $formattedSleepDuration, " +
                "sleepStartTimeMillis: $sleepStartTimeMillis, " +
                "sleepEndTimeMillis: $sleepEndTimeMillis, " +
                "sleepDurationInMillis: $sleepDurationInMillis )"

    }
}