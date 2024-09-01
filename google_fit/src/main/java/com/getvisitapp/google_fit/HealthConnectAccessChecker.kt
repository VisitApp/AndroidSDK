package com.getvisitapp.google_fit

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import com.getvisitapp.google_fit.healthConnect.contants.Contants
import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
import timber.log.Timber

class HealthConnectAccessChecker(val context: Context) {

    private val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    private suspend fun checkAvailability(): HealthConnectConnectionState {
        val availabilityStatus = checkHealthConnectAvailabilityStatus()

        return when (availabilityStatus) {

            HealthConnectClient.SDK_UNAVAILABLE -> {
                Timber.d("SDK_UNAVAILABLE")
                HealthConnectConnectionState.NOT_SUPPORTED
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Timber.d("SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
                HealthConnectConnectionState.NOT_INSTALLED
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                //Note: don't put updateButtonState(HealthConnectConnectionState.INSTALLED) here. it is the job of checkPermissionsAndRun()
                Timber.d("SDK_AVAILABLE")
                checkPermissionsAndRun()
            }

            else -> {
                HealthConnectConnectionState.NONE
            }
        }
    }

    private suspend fun checkPermissionsAndRun(): HealthConnectConnectionState {

        healthConnectClient = getHealthConnectClient()

        Timber.d("healthConnectClient hashcode: ${healthConnectClient.hashCode()}")

        val granted = healthConnectClient!!.permissionController.getGrantedPermissions()
        if (granted.containsAll(PERMISSIONS)) {
            return if (Contants.previouslyRevoked) { //special case only happens in android 14
                HealthConnectConnectionState.INSTALLED
            } else {
                HealthConnectConnectionState.CONNECTED
            }
        } else {
            Timber.d("Permission Not present")
            return HealthConnectConnectionState.INSTALLED
        }
    }

    fun checkHealthConnectAvailabilityStatus(): Int {
        //Health Connect is not available for android version below Pie (< 28)

        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        Timber.d("availabilityStatus: $availabilityStatus")

        return availabilityStatus

    }


    @Volatile
    private var healthConnectClient: HealthConnectClient? = null

    private fun getHealthConnectClient(): HealthConnectClient {
        return healthConnectClient ?: synchronized(this) {
            healthConnectClient ?: HealthConnectClient.getOrCreate(context)
                .also { healthConnectClient = it }
        }
    }

    suspend fun checkAccess(): Boolean {
        val state: HealthConnectConnectionState = checkAvailability()
        return state == HealthConnectConnectionState.CONNECTED

    }

    suspend fun revokeHealthConnectAccess() {
        checkAvailability()

        if (healthConnectClient != null) {
            healthConnectClient!!.permissionController.revokeAllPermissions()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Contants.previouslyRevoked = true
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context, "Health Connect is Available", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}