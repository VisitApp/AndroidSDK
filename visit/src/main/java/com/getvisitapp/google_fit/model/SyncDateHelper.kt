package com.getvisitapp.google_fit.model

import com.getvisitapp.google_fit.util.DateHelper
import java.util.Calendar
import java.util.Date

class SyncDateHelper {

    /**
     * if isManual==true, then endTimeStamp doesn't have any effect.
     */
    fun getSyncDates(
        startTimeStamp: Long,
        endTimeStamp: Long,
        isManual: Boolean
    ): SyncStartAndEndDate {
        if (isManual) {

            val endDateCalendar = Calendar.getInstance()
            endDateCalendar.timeInMillis = endTimeStamp

            endDateCalendar[Calendar.HOUR_OF_DAY] = 23
            endDateCalendar[Calendar.MINUTE] = 59
            endDateCalendar[Calendar.SECOND] = 59
            endDateCalendar[Calendar.MILLISECOND] = 0

            val endOfDay: Long = endDateCalendar.timeInMillis

            return SyncStartAndEndDate(startTimeStamp, endOfDay, true)
        } else {
            var startTime = startTimeStamp

            val last30Days = Calendar.getInstance()
            last30Days.time = Date()
            last30Days[Calendar.HOUR_OF_DAY] = 0
            last30Days[Calendar.MINUTE] = 0
            last30Days[Calendar.SECOND] = 0
            last30Days[Calendar.MILLISECOND] = 0
            last30Days.add(Calendar.DATE, -30)


            if (startTime == 0L) {
                startTime = last30Days.timeInMillis
            } else {
                val noOfDays =
                    DateHelper.getDifferenceBetweenTwoDays(startTime, last30Days.timeInMillis)

                if (noOfDays > 30) {
                    startTime = last30Days.timeInMillis
                } else {
                    // If the user has not updated his steps count for the first challenge then manually update his steps
                    // This will update the step count for his previous 10 days
                    // First challenge went live on 15th, so assuming that the user updates the app on 25th also, it will update his step count
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = startTime
                    calendar.add(Calendar.DATE, -1)
                    startTime = calendar.timeInMillis
                }
            }

            var endOfDay: Long = -1
            val endDateCalendar = Calendar.getInstance()
            endDateCalendar.time = Date() // compute start of the day for the timestamp
            endDateCalendar[Calendar.HOUR_OF_DAY] = 23
            endDateCalendar[Calendar.MINUTE] = 59
            endDateCalendar[Calendar.SECOND] = 59
            endDateCalendar[Calendar.MILLISECOND] = 0
            endOfDay = endDateCalendar.timeInMillis

            return SyncStartAndEndDate(startTime, endOfDay, false)
        }
    }
}

class SyncStartAndEndDate(val startTimeStamp: Long, val endTimeStamp: Long, val isManual: Boolean)