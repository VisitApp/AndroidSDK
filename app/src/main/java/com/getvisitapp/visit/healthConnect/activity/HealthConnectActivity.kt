package com.getvisitapp.visit.healthConnect.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_HISTORY
import androidx.health.connect.client.records.StepsRecord
import com.example.googlefitsdk.R
import com.getvisitapp.visit.healthConnect.data.GraphDataOperationsHelper
import com.getvisitapp.visit.healthConnect.enums.HealthConnectConnectionState
import com.getvisitapp.visit.healthConnect.helper.DailySyncManager
import com.getvisitapp.visit.healthConnect.helper.HourlySyncManager
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.DailyStepSyncRequest
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.DailySyncHealthMetric
import com.getvisitapp.visit.healthConnect.model.apiRequestModel.HourlyDataSyncRequest

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

//https://developer.android.com/reference/kotlin/androidx/health/connect/client/records/package-summary#classes

class HealthConnectActivity : AppCompatActivity() {

    val TAG = "HealthConnectActivity"
    private lateinit var initialHealthConnectButton: Button

    val graphDataOperationsHelper by lazy { GraphDataOperationsHelper(getHealthConnectClient()) }
    var dataBeyond30DaysIsAllowed: Boolean = false


    private val HEALTH_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
    )

    private val ALL_PERMISSION
        get() = if (isHistoryReadFeatureAvailable()) {
            HEALTH_PERMISSIONS.toMutableSet()
                .apply { add(PERMISSION_READ_HEALTH_DATA_HISTORY) }
        } else {
            HEALTH_PERMISSIONS.toMutableSet()
        }

    private fun isHistoryReadFeatureAvailable(): Boolean {
        return getHealthConnectClient().features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    private fun updateHistoryReadAccess(grantedPermissions: Set<String>) {
        dataBeyond30DaysIsAllowed = isHistoryReadFeatureAvailable() &&
                grantedPermissions.contains(PERMISSION_READ_HEALTH_DATA_HISTORY)
    }

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, throwable ->
            Timber.d("HealthConnectActivity coroutineExceptionHandler")
            throwable.printStackTrace()


            val formattedMessage = "${throwable.javaClass}: ${throwable.message}"
            Timber.d("mytag: $formattedMessage")


        }

    val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)


    val requestPermissionActivityContract: ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()


    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
            if (granted.containsAll(HEALTH_PERMISSIONS)) {
                Timber.d("Permissions successfully granted: ${ALL_PERMISSION}")

                updateButtonState(HealthConnectConnectionState.CONNECTED)
                scope.launch {
                    checkPermissionsAndRun()
                }

            } else {
                Timber.d(" Lack of required permissions")

                //Currently the Health Connect SDK, only asks for the remaining permission was the NOT granted in the first time, and when it return,
                //it also send the granted permission (and not the permission that was previously granted), so the control flow comes inside the else statement.
                //So we need to check for permission again
                scope.launch {
                    checkPermissionsAndRun()
                }
            }
        }

    var healthConnectConnectionState: HealthConnectConnectionState =
        HealthConnectConnectionState.NONE


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_health_connect)

        val checkHealthConnectAvailabilityStatusButton =
            findViewById<Button>(R.id.checkHealthConnectAvailabilityStatus)
        initialHealthConnectButton = findViewById(R.id.initialHealthConnect)
        val openHealthConnectAppButton = findViewById<Button>(R.id.openHealthConnectApp)
        val removeHealthConnectPermissionButton =
            findViewById<Button>(R.id.removeHealthConnectPermission)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        updateButtonState(HealthConnectConnectionState.NONE)



        checkHealthConnectAvailabilityStatusButton.setOnClickListener {
            checkAvailability()
        }

        initialHealthConnectButton.setOnClickListener {

            when (healthConnectConnectionState) {
                HealthConnectConnectionState.NOT_INSTALLED -> {
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.google.android.apps.healthdata")
                            )
                        )
                    } catch (exception: ActivityNotFoundException) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                            )
                        )
                    }
                }


                HealthConnectConnectionState.INSTALLED -> {
                    requestPermissions.launch(ALL_PERMISSION)
                }

                HealthConnectConnectionState.CONNECTED -> {
                    scope.launch {
                        checkPermissionsAndRun()
                    }

                }

                HealthConnectConnectionState.NOT_SUPPORTED, HealthConnectConnectionState.NONE -> {
                    //do nothing for now.
                }

            }
        }

        openHealthConnectAppButton.setOnClickListener {
            val settingsIntent = Intent()
            settingsIntent.action = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
            startActivity(settingsIntent)
        }

        removeHealthConnectPermissionButton.setOnClickListener {
            scope.launch {
                if (healthConnectClient != null) {
                    healthConnectClient!!.permissionController.revokeAllPermissions()
                    checkAvailability()
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            this@HealthConnectActivity,
                            "healthConnectClient not initialized",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    fun checkAvailability() {
        val availabilityStatus = checkHealthConnectAvailabilityStatus()

        when (availabilityStatus) {

            HealthConnectClient.SDK_UNAVAILABLE -> {
                Timber.d("SDK_UNAVAILABLE")
                updateButtonState(HealthConnectConnectionState.NOT_SUPPORTED)
            }

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                Timber.d("SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED")
                updateButtonState(HealthConnectConnectionState.NOT_INSTALLED)
            }

            HealthConnectClient.SDK_AVAILABLE -> {
                Timber.d("SDK_AVAILABLE")
                scope.launch {
                    checkPermissionsAndRun()
                }
            }

            else -> {
                Timber.d("availabilityStatus else")
            }
        }
    }

    fun checkHealthConnectAvailabilityStatus(): Int {
        //Health Connect is not available for android version below Pie (< 28)

        val availabilityStatus = HealthConnectClient.getSdkStatus(this)
        Timber.d("availabilityStatus: $availabilityStatus")

        return availabilityStatus

    }

    fun updateButtonState(healthConnectConnectionState: HealthConnectConnectionState): String {
        this.healthConnectConnectionState = healthConnectConnectionState

        val text = when (healthConnectConnectionState) {
            HealthConnectConnectionState.NOT_SUPPORTED -> "Not Supported"
            HealthConnectConnectionState.NOT_INSTALLED -> "Install Health Connect"
            HealthConnectConnectionState.CONNECTED -> "Connected"
            HealthConnectConnectionState.INSTALLED -> "Not Connected"
            else -> "Unknown"
        }

        Handler(Looper.getMainLooper()).post {
            initialHealthConnectButton.text = text
        }

        return text
    }


    @Volatile
    private var healthConnectClient: HealthConnectClient? = null

    private fun getHealthConnectClient(): HealthConnectClient {
        return healthConnectClient ?: synchronized(this) {
            healthConnectClient ?: HealthConnectClient.getOrCreate(this)
                .also { healthConnectClient = it }
        }
    }


    private suspend fun checkPermissionsAndRun() {

        healthConnectClient = getHealthConnectClient()

        val granted = healthConnectClient!!.permissionController.getGrantedPermissions()
        updateHistoryReadAccess(granted)

        Timber.tag("mytag").d(
            "healthConnectClient hashcode: ${healthConnectClient.hashCode()}, " +
                    "historyFeatureAvailable: ${isHistoryReadFeatureAvailable()}, " +
                    "dataBeyond30DaysIsAllowed: $dataBeyond30DaysIsAllowed, granted: $granted"
        )

        if (granted.containsAll(HEALTH_PERMISSIONS)) {

            updateButtonState(HealthConnectConnectionState.CONNECTED)

            // Permissions already granted; proceed with inserting or reading data

            Timber.d("All Permission Allowed")

            var timeStamp = 1782906461000L //current time
//            var timeStamp = 1724424597000L // one week before time

            scope.launch {

//                Tutorials(healthConnectClient!!).fetchData()

                getDailySyncData(
                    timeStamp = timeStamp,
                    dataBeyond30DaysIsAllowed = dataBeyond30DaysIsAllowed
                )
//                getHourlySyncData(timeStamp)
//                exhaustHealthConnectQueryLimitTest(timeStamp)

//                getActivityData(type = "steps", frequency = "day", timeStamp = timeStamp)
//                getActivityData(type = "steps", frequency = "week", timeStamp = timeStamp)
//                getActivityData(type = "steps", frequency = "month", timeStamp = timeStamp)
            }
        } else {
            Timber.d("Permission Not present. granted: $granted")
            updateButtonState(HealthConnectConnectionState.INSTALLED)
        }
    }

    // For the dashboard graph.
    private fun getDailyStepAndSleepData() {
        scope.launch {
            try {
                val resultString =
                    graphDataOperationsHelper.getTodayStepsAndSleepData(getHealthConnectClient())


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    //2.For all the details graph.
    private fun getActivityData(type: String?, frequency: String?, timeStamp: Long) {
        if (type != null && frequency != null) {
            when (type) {
                "steps" -> {
                    when (frequency) {
                        "day" -> {
                            scope.launch {
                                try {
                                    val resultString =
                                        graphDataOperationsHelper.getDailyStepsData(timeStamp)


                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }


                        }

                        "week" -> {
                            scope.launch {
                                try {
                                    val resultString =
                                        graphDataOperationsHelper.getWeeklyStepsData(timeStamp)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }

                        "month" -> {
                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getMonthlyStepsData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }
                    }
                }

                "distance" -> {
                    when (frequency) {
                        "day" -> {
                            scope.launch {
                                try {
                                    val resultString =
                                        graphDataOperationsHelper.getDailyDistanceData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }

                        "week" -> {
                            scope.launch {
                                try {
                                    val resultString =
                                        graphDataOperationsHelper.getWeeklyDistanceData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }

                        "month" -> {
                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getMonthlyDistanceData(timeStamp)


                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }
                    }
                }

                "calories" -> {
                    when (frequency) {
                        "day" -> {

                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getDailyCalorieData(timeStamp)


                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        "week" -> {
                            scope.launch {
                                try {
                                    val resultString =
                                        graphDataOperationsHelper.getWeeklyCalorieData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        "month" -> {
                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getMonthlyCalorieData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }
                    }
                }

                "sleep" -> {
                    when (frequency) {
                        "day" -> {
                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getDailySleepData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }

                        "week" -> {
                            scope.launch {
                                try {

                                    val resultString =
                                        graphDataOperationsHelper.getWeeklySleepData(timeStamp)

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                        }
                    }
                }
            }
        }

    }


    suspend fun getDailySyncData(
        timeStamp: Long,
        dataBeyond30DaysIsAllowed: Boolean
    ): DailyStepSyncRequest {

        val dailySyncManager = DailySyncManager(getHealthConnectClient())

        val dailySyncData: List<DailySyncHealthMetric> =
            dailySyncManager.getDailySyncData(
                dailyLastSyncTimeStamp = timeStamp,
                dataBeyond30DaysIsAllowed = dataBeyond30DaysIsAllowed
            )

        val requestBody = DailyStepSyncRequest(fitnessData = dailySyncData, platform = "ANDROID")

        Timber.d("getDailySyncData: requestBody: ${Gson().toJson(requestBody)}")

        return requestBody
    }

    suspend fun exhaustHealthConnectQueryLimitTest(timeStamp: Long) {
        (1..1000).forEachIndexed { index, i ->
            Timber.d("exhaustHealthConnectQueryLimitTest: index: $index")
            getHourlySyncData(timeStamp)
        }
    }

    suspend fun getHourlySyncData(timeStamp: Long): HourlyDataSyncRequest {

        val hourlySyncManager = HourlySyncManager(getHealthConnectClient())

        val hourlyRecords = hourlySyncManager.getHourlySyncData(hourlyLastSyncTimestamp = timeStamp)

        val requestBody =
            HourlyDataSyncRequest(bulkHealthData = hourlyRecords, platform = "ANDROID")


        Timber.d("getHourlySyncData: requestBody: ${Gson().toJson(requestBody)}")

        return requestBody
    }


}
