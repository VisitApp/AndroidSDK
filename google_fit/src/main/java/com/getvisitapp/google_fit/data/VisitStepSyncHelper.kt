package com.getvisitapp.google_fit.data

import android.content.Context
import androidx.annotation.Keep

@Keep
class VisitStepSyncHelper {


    companion object {
        fun Context.openGoogleFit(): Boolean {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
            return if (launchIntent != null) {
                startActivity(launchIntent)
                true;
            } else {
                false;
            }
        }
    }


}