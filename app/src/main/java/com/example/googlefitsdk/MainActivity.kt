package com.example.googlefitsdk

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Button
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.getvisitapp.google_fit.HealthConnectAccessChecker
import com.getvisitapp.google_fit.IntiateSdk
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.data.VisitStepSyncHelper.Companion.openGoogleFit
import com.getvisitapp.google_fit.event.ClosePWAEvent
import com.getvisitapp.google_fit.event.MessageEvent
import com.getvisitapp.google_fit.event.VisitEventType
import com.getvisitapp.google_fit.healthConnect.activity.HealthConnectActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber


class MainActivity : AppCompatActivity() {
    private var TAG = "mytag10"

    private val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"

    private lateinit var healthConnectAccessChecker: HealthConnectAccessChecker
    private lateinit var healthConnectAndFitBitSwitch: SwitchMaterial

    private val tataAIG_base_url = "https://uathealthvas.tataaig.com"
    private val tataAIG_auth_token = "Basic Z2V0X3Zpc2l0OkZoNjh2JHdqaHU4WWd3NiQ="

    private lateinit var syncStepHelper: VisitStepSyncHelper

    val job = Job()
    var coroutineScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Timber.plant(Timber.DebugTree())

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }


        healthConnectAndFitBitSwitch = findViewById(R.id.googleFitSwitch)
        healthConnectAccessChecker = HealthConnectAccessChecker(this)

        syncStepHelper = VisitStepSyncHelper(context = this, default_client_id)


        findViewById<Button>(R.id.manualSyncStepButton).setOnClickListener {
            syncStepHelper.syncSteps(tataAIG_base_url, tataAIG_auth_token)
        }


        //Open Google Fit if installed else return false.
        findViewById<Button>(R.id.openGoogleFitApp).setOnClickListener {
            val result = openGoogleFit()
            if (result) {
                //do nothing
            } else {
                Toast.makeText(this, "Google Fit app is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.hraIncomplete).setOnClickListener {
            syncStepHelper.sendHRAInComplete(tataAIG_base_url, tataAIG_auth_token)
        }

        findViewById<Button>(R.id.revokeFitBitAccessButton).setOnClickListener {
            syncStepHelper.revokeFitbitAccess()
        }

        findViewById<Button>(R.id.openHealthConnectButton).setOnClickListener {
            val intent = Intent(this, HealthConnectActivity::class.java)
            startActivity(intent)
        }
    }

    fun setHealthAppStatus(healthConnectEnabled: Boolean, fitbitEnabled: Boolean) {

        healthConnectAndFitBitSwitch.setText(
            "Health Connect / Fitbit",
            TextView.BufferType.SPANNABLE
        )


        val spannable = healthConnectAndFitBitSwitch.text as Spannable


        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    healthConnectAndFitBitSwitch.context,
                    R.color.redColor
                )
            ), 0, healthConnectAndFitBitSwitch.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (healthConnectEnabled) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        healthConnectAndFitBitSwitch.context,
                        R.color.greenColor
                    )
                ), 0, 14, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (fitbitEnabled) {
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        healthConnectAndFitBitSwitch.context,
                        R.color.greenColor
                    )
                ), 11, healthConnectAndFitBitSwitch.text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        spannable.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(
                    healthConnectAndFitBitSwitch.context,
                    R.color.greyColor
                )
            ), 16, 17, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: MessageEvent?) {
        event?.let { eventType ->
            Log.d(TAG, "event:${event.eventType}")
            when (eventType.eventType) {
                VisitEventType.AskForFitnessPermission -> {

                }

                VisitEventType.AskForLocationPermission -> {


                }

                is VisitEventType.FitnessPermissionGranted -> {
                    val data = event.eventType as VisitEventType.FitnessPermissionGranted

                    Log.d(
                        "mytag",
                        "MainActivity onMessageEvent() FitnessPermissionGranted called, isGoogleFit/Fitbit: ${data.isGoogleFit}"
                    )


                    var isGoogleFitConnected = data.isGoogleFit


                }

                is VisitEventType.FitnessPermissionRevoked -> {
                    val data = event.eventType as VisitEventType.FitnessPermissionRevoked

                    Log.d(
                        "mytag",
                        "MainActivity onMessageEvent() FitnessPermissionRevoked called, isGoogleFit: ${data.isGoogleFit}"
                    )

                }

                is VisitEventType.RequestHealthDataForDetailedGraph -> {

                    val graphEvent =
                        event.eventType as VisitEventType.RequestHealthDataForDetailedGraph

                }

                is VisitEventType.StartVideoCall -> {
                    val callEvent =
                        event.eventType as VisitEventType.StartVideoCall


                }

                is VisitEventType.HRA_Completed -> {


                }

                is VisitEventType.GoogleFitConnectedAndSavedInPWA -> {
                    Handler(Looper.getMainLooper()).postDelayed({
                        //passing event to Visit PWA to close itself
                        EventBus.getDefault().post(ClosePWAEvent())


                    }, 200)
                }

                is VisitEventType.HRAQuestionAnswered -> {
                    // can be used for analytics events
                    val hraQuestionEvent = event.eventType as VisitEventType.HRAQuestionAnswered
                    Log.d(
                        "mytag",
                        "current:${hraQuestionEvent.current} total:${hraQuestionEvent.total}"
                    )
                }

                VisitEventType.ConsultationBooked -> {
                    Log.d("mytag", "MainActivity ConsultationBooked event")
                }

                VisitEventType.CouponRedeemed -> {
                    Log.d("mytag", "MainActivity CouponRedeemed event")
                }

                is VisitEventType.FitnessPermissionError -> {
                    val eventData = event.eventType as VisitEventType.FitnessPermissionError

                    Log.d("mytag", "eventData: ${eventData.message}")

                }

                is VisitEventType.StepSyncError -> {
                    val eventData = event.eventType as VisitEventType.StepSyncError

                    Log.d("mytag", "eventData: ${eventData.message}")
                }

                is VisitEventType.NetworkError -> {
                    val eventData = event.eventType as VisitEventType.NetworkError


                    val errStatus = eventData.errStatus
                    val error = eventData.error

                    Log.d("mytag", "errStatus: $errStatus, error: $error")

                }

                is VisitEventType.VisitCallBack -> {
                    val eventData = event.eventType as VisitEventType.VisitCallBack

                    val message: String = eventData.message

                    val failureReason: String? = eventData.failureReason

                    Log.d("mytag", "VisitCallBack message: $message, failureReason: $failureReason")

                }
            }

        }

    }

    fun init() {

//        val magicLink =
//            "https://web.getvisitapp.xyz/"

        val magicLink =
            "https://tata-aig.getvisitapp.com/sso?userParams=XsOIXYsDhmOiQpvGWzKerumQP_5AIWLHz5y6VOB22tO2eiPMsKCDvXB1hR9YG8WteES1e51ztX7xjtCjyiW4bnWXI4wn3g1wPJJKOLXyz2IALAp51GOrhRtVQG4MzsBNMSmimkeiwB3xtn_GrIR9j1ePrEKx4uy5qh9zhvitdOn3Hu9MDB-Vgd9Kb7z854hbMSlKlDuzQqVSxvY74DVYRKhL_gEkuHXoeMjJy9bEcx-l98ZIOOyhK4hpksNIOytD7lWzRvErxJNXm1OboaB0QDtj5P3-jJ2hLRKwcIrLbdEvXSZrqWF3vPfyHDimzPEh&clientId=tata-aig-a8b455"


//        val magicLink =
//            "https://star-health.getvisitapp.com/?mluib7c=QNkg98jB"


        IntiateSdk.s(
            this,
            false,
            magicLink,
            tataAIG_base_url,
            tataAIG_auth_token,
            default_client_id
        )
    }

    override fun onStart() {
        super.onStart()

        //unregister first before registering again.
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        EventBus.getDefault().register(this)
    }


    override fun onResume() {
        super.onResume()

        healthConnectAndFitBitSwitch.setOnCheckedChangeListener(null)

        coroutineScope.launch {
            try {
                val healthConnectAccess = healthConnectAccessChecker.checkAccess();
                val fitBitAccess = syncStepHelper.getFitbitCurrentStatus()

                Log.d(
                    "mytag",
                    "healthConnectAccess: $healthConnectAccess && fitBitAccess:$fitBitAccess"
                )

                healthConnectAndFitBitSwitch.isChecked = healthConnectAccess || fitBitAccess

                //updating the UI
                setHealthAppStatus(
                    healthConnectAccess, fitBitAccess,
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }




        healthConnectAndFitBitSwitch.setOnCheckedChangeListener(googleFitCheckChangeListener)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        job.cancel()
        super.onDestroy()
    }

    var googleFitCheckChangeListener = object : OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
            if (isChecked) {
                init()
            } else {
                if (syncStepHelper.getFitbitCurrentStatus()) {
                    syncStepHelper.revokeFitbitAccess()
                } else {
                    coroutineScope.launch {
                        healthConnectAccessChecker.revokeHealthConnectAccess()
                    }
                }

                //updating the UI.
                setHealthAppStatus(false, false)
            }
        }
    }

}

