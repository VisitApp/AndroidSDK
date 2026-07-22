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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Keep
class VisitStepSyncHelper(var context: Context) {


    private fun getVisitApiService(baseUrl: String, visitAuthToken: String): ApiService {

        Timber.d("mytag: getVisitApiService authToken: $visitAuthToken, baseUrl: $baseUrl")

        return APIServiceInstance.getApiService(
            baseUrl, context, visitAuthToken, true
        )
    }

    val coroutineExceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Timber.d("HealthConnectUtil coroutineExceptionHandler")
        throwable.printStackTrace()
    }


    fun sendDataToVisitServer(
        healthConnectUtil: HealthConnectUtil,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long,
        visitBaseUrl: String,
        visitAuthToken: String
    ) {

        Timber.d("sendDataToVisitServer: googleFitLastSync: $googleFitLastSync, gfHourlyLastSync: $gfHourlyLastSync")

        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            try {
                if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
                    val dailySyncRequestBody = healthConnectUtil.getDailySyncData(googleFitLastSync)
                    val dailySyncResponse = syncDailyHealthData(
                        dailyStepSyncRequest = dailySyncRequestBody,
                        visitBaseUrl = visitBaseUrl,
                        visitAuthToken = visitAuthToken
                    )

                    Timber.d("dailySyncResponse: $dailySyncResponse")

                    if (dailySyncResponse?.message == "success") {

                    } else {

                    }

                    val hourlyDataSyncRequestBody =
                        healthConnectUtil.getHourlySyncData(gfHourlyLastSync)

                    val hourlySyncResponse = syncHourlyHealthData(
                        hourlyDataSyncRequest = hourlyDataSyncRequestBody,
                        visitBaseUrl = visitBaseUrl,
                        visitAuthToken = visitAuthToken
                    )

                    Timber.d("hourlySyncResponse: $hourlySyncResponse")

                    if (hourlySyncResponse?.message == "success") {

                    } else {

                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
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


}