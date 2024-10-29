package com.example.googlefitsdk

import android.app.Application

class SdkApp : Application() {
    override fun onCreate() {
        super.onCreate()

        TimberUtils.configTimber()
    }
}