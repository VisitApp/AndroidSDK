package com.getvisitapp.visit.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


@Keep
data class LocationCoordinate(val lat: Double, val lng: Double)

@Keep
class LocationTrackerUtil(
    private var context: Context,
) {

    private var TAG = "LocationTrackerUtil"

    private lateinit var locationManager: LocationManager


    fun isGPSEnabled(): Boolean {
        //checking if GPS is enabled or not
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun isPreciseLocationPermissionAllowed(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationPermissionAllowed(): Boolean {
        //checking if both fine and coarse location is present or not
        return (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED) && (ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }


    fun showGPS_NotEnabledDialog() {
        val alertDialog = AlertDialog.Builder(context)

        // Setting Dialog Title
        alertDialog.setTitle("Turn on GPS")

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

        // On pressing Settings button
        alertDialog.setPositiveButton(
            "Settings"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }

    fun showLocationPermissionDeniedAlertDialog() {
        val alertDialog = AlertDialog.Builder(context)

        // Setting Dialog Title
        alertDialog.setTitle("Allow Location Permission")

        // Setting Dialog Message
        alertDialog.setMessage("Location Permission is not enabled. Do you want to go to settings menu?")

        // On pressing Settings button
        alertDialog.setPositiveButton(
            "Settings"
        ) { dialog, which ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts(
                "package", context.applicationContext.packageName, null
            )
            intent.data = uri
            context.startActivity(intent)
        }

        // on pressing cancel button
        alertDialog.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }
        alertDialog.show()
    }

    fun promptUserToTurnOnGPS(
        onSuccessListener: () -> Unit,
        onResolutionRequiredListener: (intentSenderRequest: IntentSenderRequest) -> Unit,
        onFailureListener: (exception: Exception) -> Unit,
    ) {
        val activity = context as? Activity
        if (activity == null) {
            onFailureListener(IllegalStateException("Location settings prompt requires an Activity context."))
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 1000)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(2 * 1000)
            .setMaxUpdateDelayMillis(5 * 1000)
            .build()

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
            .build()

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> =
            settingsClient.checkLocationSettings(locationSettingsRequest)

        task.addOnSuccessListener(activity) {
            onSuccessListener()
        }

        task.addOnFailureListener(activity) { exception ->
            if (exception is ResolvableApiException) {
                try {
                    onResolutionRequiredListener(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (sendIntentException: IntentSender.SendIntentException) {
                    onFailureListener(sendIntentException)
                }
            } else {
                onFailureListener(exception)
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun awaitSingleHighAccuracyLocation(
        timeoutMillis: Long = 10_000L
    ): LocationCoordinate {
        if (!isPreciseLocationPermissionAllowed()) {
            throw SecurityException("Location permission not granted.")
        }

        if (!isGPSEnabled()) {
            throw IllegalStateException("GPS is disabled.")
        }

        return withTimeout(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val locationRequest =
                    LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 1000)
                        .setWaitForAccurateLocation(true)
                        .setMinUpdateIntervalMillis(2 * 1000)
                        .setMaxUpdateDelayMillis(5 * 1000)
                        .build()

                var locationCallback: LocationCallback? = null

                fun stopLocationUpdates() {
                    locationCallback?.let { callback ->
                        fusedLocationClient.removeLocationUpdates(callback)
                    }
                    locationCallback = null
                }

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        val location =
                            locationResult.lastLocation ?: locationResult.locations.firstOrNull()
                        stopLocationUpdates()

                        if (!continuation.isActive) {
                            return
                        }

                        if (location != null) {
                            continuation.resume(
                                LocationCoordinate(
                                    lat = location.latitude,
                                    lng = location.longitude
                                )
                            )
                        } else {
                            continuation.resumeWithException(
                                IllegalStateException("Unable to get your current location.")
                            )
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    stopLocationUpdates()
                }

                try {
                    val activeLocationCallback = locationCallback
                        ?: throw IllegalStateException("Location callback was not created.")

                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        activeLocationCallback,
                        Looper.getMainLooper()
                    ).addOnFailureListener { exception ->
                        stopLocationUpdates()
                        if (continuation.isActive) {
                            continuation.resumeWithException(exception)
                        }
                    }
                } catch (exception: Exception) {
                    stopLocationUpdates()
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            }
        }
    }




}
