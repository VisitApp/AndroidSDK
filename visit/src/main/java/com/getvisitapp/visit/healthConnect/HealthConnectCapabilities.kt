package com.getvisitapp.visit.healthConnect

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.Keep

@Keep
object HealthConnectCapabilities {

    internal const val MIN_NATIVE_STEP_TRACKING_EXTENSION = 20

    @JvmStatic
    fun isNativeStepTrackingAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }

        val extensionVersion =
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        return isNativeStepTrackingAvailable(Build.VERSION.SDK_INT, extensionVersion)
    }

    internal fun isNativeStepTrackingAvailable(
        sdkInt: Int,
        extensionVersion: Int
    ): Boolean {
        return sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            extensionVersion >= MIN_NATIVE_STEP_TRACKING_EXTENSION
    }
}
