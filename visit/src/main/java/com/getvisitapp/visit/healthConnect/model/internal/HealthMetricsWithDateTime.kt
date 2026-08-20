package com.getvisitapp.visit.healthConnect.model.internal

import androidx.annotation.Keep
import java.time.LocalDateTime

@Keep
data class HealthMetricsWithDateTime(
    var steps: Long? = null,
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
