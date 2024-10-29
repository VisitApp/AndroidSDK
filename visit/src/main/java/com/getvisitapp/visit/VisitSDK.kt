package com.getvisitapp.visit

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.getvisitapp.visit.activity.SdkWebviewActivity

@Keep
object VisitSDK {

    fun init(c: Context, isDebug: Boolean, magicLink: String) {
        val intent: Intent = SdkWebviewActivity.getIntent(c, isDebug, magicLink)
        c.startActivity(intent)
    }

    fun setUserEventCallback(callback: (eventName: String) -> Unit) {
        SdkWebviewActivity.userEventCallback = callback
    }

    fun setErrorEventCallback(callback: (errorMessage: String, description: String?) -> Unit) {
        SdkWebviewActivity.errorEventCallback = callback
    }
}
