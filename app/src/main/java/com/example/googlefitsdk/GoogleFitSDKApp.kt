package com.example.googlefitsdk

import android.app.Application
import timber.log.Timber

class GoogleFitSDKApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}