package com.getvisitapp.visit

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import com.getvisitapp.visit.activity.PaymentActivity

@Keep
object VisitSDK {

    fun init(c: Context, magicLink: String,token:String) {
        val intent: Intent = PaymentActivity.getIntent(c, magicLink,token)
        c.startActivity(intent)
    }
}
