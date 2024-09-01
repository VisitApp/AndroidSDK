package com.getvisitapp.google_fit.network

import androidx.annotation.Keep
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest
import com.getvisitapp.google_fit.healthConnect.model.apiRequestModel.SyncResponse
import com.getvisitapp.google_fit.model.FitBitRevokeResponse
import com.getvisitapp.google_fit.model.FitbitDataResponse
import com.getvisitapp.google_fit.model.SessionRoom
import com.getvisitapp.google_fit.model.TataAIGFitnessPayload
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

@Keep
interface ApiService {

    @GET("video-call/video-session-info")
    suspend fun getRoomDetails(
        @Query("sessionId") sessionId: Int,
        @Query("consultationId") consultationId: Int
    ): SessionRoom


    @GET("fitness/current-progress/fitbit")
    suspend fun getFitBitStatus(
        @Query("start") start: Long,
        @Query("end") end: Long,
        @Query("version") version: Long = 1
    ): FitbitDataResponse

    @POST("fitness-activity")
    suspend fun pushDataToTataAIG(@Body tataAIGFitnessPayload: TataAIGFitnessPayload): JsonObject

    @POST("wearables/fitbit/revoke")
    suspend fun revokeFitBitAccess(): FitBitRevokeResponse


    @POST("users/data-sync")
    suspend fun uploadDailyHealthData(
        @Query("isPWA") isPWA: String = "yes",
        @Body requestBody: DailyStepSyncRequest
    ): SyncResponse?

    @POST(" users/embellish-sync")
    suspend fun uploadHourlyHealthData(
        @Query("isPWA") isPWA: String = "yes",
        @Body requestBody: HourlyDataSyncRequest
    ): SyncResponse?

}