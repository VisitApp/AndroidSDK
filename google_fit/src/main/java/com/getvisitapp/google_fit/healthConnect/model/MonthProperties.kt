package com.getvisitapp.google_fit.healthConnect.model

import java.time.LocalDate


data class MonthProperties(
    val firstDayOfMonth: LocalDate,
    val lastDayOfMonth: LocalDate,
    val numberOfDaysInMonth: Int
)