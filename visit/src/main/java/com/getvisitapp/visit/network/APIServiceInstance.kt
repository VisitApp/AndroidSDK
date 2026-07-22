package com.getvisitapp.visit.network

import android.content.Context
import androidx.annotation.Keep


@Keep
object APIServiceInstance {

    fun getApiService(
        baseUrl: String,
        applicationContext: Context,
        authToken: String,
        isTimeOutEnable: Boolean = true
    ): ApiService {
        return RetrofitBuilder.getRetrofit(baseUrl, applicationContext, authToken, isTimeOutEnable)
            .create(ApiService::class.java)
    }
}
