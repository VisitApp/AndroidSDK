package com.getvisitapp.google_fit.healthConnect.model

/**
 * {
 *   "fitnessData": [
 *     {
 *       "steps": 202,
 *       "calorie": 1470,
 *       "distance": 76,
 *       "activity": 107,
 *       "sleep": "1724603400000-1724635800000",
 *       "date": 1724610600000
 *     },
 *     {
 *       "steps": 821,
 *       "calorie": 1488,
 *       "distance": 348,
 *       "activity": 438,
 *       "date": 1724697000000
 *     }
 *   ]
 * }
 */

data class DailyStepSyncRequestBody(
    val fitnessData: List<DailySyncHealthMetric>,
    val platform: String
)