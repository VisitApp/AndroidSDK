package com.getvisitapp.google_fit.healthConnect.model

import java.time.LocalDateTime

data class HealthMetricsWithDateTime(
    var steps: Long? = null,
    var distance: Double? = null,
    var calorie: Double? = null,
    val dateTime: LocalDateTime,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HealthMetricsWithDateTime) return false

        return dateTime == other.dateTime
    }

    override fun hashCode(): Int {
        return 31 * dateTime.hashCode()
    }
}