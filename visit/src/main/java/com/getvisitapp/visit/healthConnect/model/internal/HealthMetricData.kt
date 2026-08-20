package com.getvisitapp.visit.healthConnect.model.internal

import androidx.annotation.Keep
import java.time.Duration


/**
 * Think of this class like this:
 * HealthMetricData will contain the summation of the health data for the time period
 * and `healthMetricWithDateTime` will contain the entries of the things that contributed to that summation
 */

@Keep
data class HealthMetricData(
    var healthMetricWithDateTime: List<HealthMetricsWithDateTime>? = null,

    var totalSteps: Long? = null,

    var averageSteps: Long? = null,

    var totalActivityTime: Duration? = null,
    var averageActivityTime: Duration? = null,
)
