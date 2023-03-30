package com.getvisitapp.google_fit.data

internal interface FitnessDataHelperListener {
    fun setDailyFitnessDataJSON(data: String?) //data is in the form of JSON
    fun setHourlyFitnessDataJSON(data: String?) //data is in the form of JSON
}