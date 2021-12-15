package com.getvisitapp.google_fit.network

import com.getvisitapp.google_fit.model.SessionRoom
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("video-call/video-session-info")
    suspend fun getRoomDetails(
        @Query("sessionId") sessionId: Int,
        @Query("consultationId") consultationId: Int
    ): SessionRoom


}