package com.getvisitapp.visit.data

import android.content.Context
import androidx.annotation.Keep
import com.getvisitapp.visit.healthConnect.activity.HealthConnectUtil
import com.getvisitapp.visit.healthConnect.enums.HealthConnectConnectionState
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.SyncResponse
import com.getvisitapp.visit.network.APIServiceInstance
import com.getvisitapp.visit.network.ApiService
import kotlinx.coroutines.launch
import timber.log.Timber

@Keep
class VisitStepSyncHelper(var context: Context) {


    private fun getVisitApiService(baseUrl: String, visitAuthToken: String): ApiService {

        Timber.d("mytag: getVisitApiService baseUrl: $baseUrl")

        return APIServiceInstance.getApiService(
            normalizeBaseUrl(baseUrl), context, visitAuthToken, true
        )
    }

    fun sendDataToVisitServer(
        healthConnectUtil: HealthConnectUtil,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long,
        visitBaseUrl: String,
        visitAuthToken: String,
        onSuccess: ((String) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {

        Timber.d("sendDataToVisitServer: googleFitLastSync: $googleFitLastSync, gfHourlyLastSync: $gfHourlyLastSync")

        healthConnectUtil.scope.launch {
            try {
                if (healthConnectUtil.healthConnectConnectionState != HealthConnectConnectionState.CONNECTED) {
                    val reason = "Health Connect is not connected"
                    Timber.d("sendDataToVisitServer skipped: $reason")
                    onFailure?.invoke(reason)
                    return@launch
                }

                if (visitBaseUrl.isBlank() || visitAuthToken.isBlank()) {
                    val reason = "Visit sync credentials are missing"
                    Timber.d("sendDataToVisitServer skipped: $reason")
                    onFailure?.invoke(reason)
                    return@launch
                }

                val normalizedVisitBaseUrl = normalizeBaseUrl(visitBaseUrl)
                val dailySyncRequestBody = healthConnectUtil.getDailySyncData(googleFitLastSync)
                val dailySyncResponse = syncDailyHealthData(
                    dailyStepSyncRequest = dailySyncRequestBody,
                    visitBaseUrl = normalizedVisitBaseUrl,
                    visitAuthToken = visitAuthToken
                )

                Timber.d("dailySyncResponse: $dailySyncResponse")

                if (!dailySyncResponse.isSuccess()) {
                    onFailure?.invoke(dailySyncResponse.failureReason("Daily health data sync failed"))
                    return@launch
                }

                val hourlyDataSyncRequestBody =
                    healthConnectUtil.getHourlySyncData(gfHourlyLastSync)

                val hourlySyncResponse = syncHourlyHealthData(
                    hourlyDataSyncRequest = hourlyDataSyncRequestBody,
                    visitBaseUrl = normalizedVisitBaseUrl,
                    visitAuthToken = visitAuthToken
                )

                Timber.d("hourlySyncResponse: $hourlySyncResponse")

                if (!hourlySyncResponse.isSuccess()) {
                    onFailure?.invoke(hourlySyncResponse.failureReason("Hourly health data sync failed"))
                    return@launch
                }

                onSuccess?.invoke("Health data sync completed")
            } catch (e: Exception) {
                e.printStackTrace()
                onFailure?.invoke(e.message ?: "Health data sync failed")
            }

        }


    }


    private suspend fun syncDailyHealthData(
        dailyStepSyncRequest: DailyStepSyncRequest, visitBaseUrl: String, visitAuthToken: String
    ): SyncResponse? {
        val visitApiService = getVisitApiService(visitBaseUrl, visitAuthToken)
        val response = visitApiService.uploadDailyHealthData(requestBody = dailyStepSyncRequest)

        return response
    }

    private suspend fun syncHourlyHealthData(
        hourlyDataSyncRequest: HourlyDataSyncRequest,
        visitBaseUrl: String, visitAuthToken: String
    ): SyncResponse? {

        val visitApiService = getVisitApiService(visitBaseUrl, visitAuthToken)

        val response = visitApiService.uploadHourlyHealthData(requestBody = hourlyDataSyncRequest)

        return response
    }

    private fun normalizeBaseUrl(baseUrl: String): String {
        val trimmedBaseUrl = baseUrl.trim()
        return if (trimmedBaseUrl.endsWith("/")) trimmedBaseUrl else "$trimmedBaseUrl/"
    }

    private fun SyncResponse?.isSuccess(): Boolean {
        return this?.message?.equals("success", ignoreCase = true) == true
    }

    private fun SyncResponse?.failureReason(defaultReason: String): String {
        return this?.errorMessage?.takeIf { it.isNotBlank() }
            ?: this?.message?.takeIf { it.isNotBlank() }?.let { "$defaultReason: $it" }
            ?: defaultReason
    }


}