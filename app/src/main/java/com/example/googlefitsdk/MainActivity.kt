package com.example.googlefitsdk

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
import com.getvisitapp.google_fit.IntiateSdk
import com.getvisitapp.google_fit.event.ClosePWAEvent
import com.getvisitapp.google_fit.event.MessageEvent
import com.getvisitapp.google_fit.event.VisitEventType
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.google.android.material.switchmaterial.SwitchMaterial
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {
    private var TAG = "mytag10"

    private val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"

    private lateinit var googleFitAccessChecker: GoogleFitAccessChecker



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

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
            "https://tata-aig.getvisitapp.xyz/sso?userParams=LH53MbTMvD-UcbZxap-i6S-h-D2Y0JgBpwJdDPhiJGjGsqYHDbZ0C3hZruy4_7BcCkS5elSq_4Xc3oIaieYs7pvT5dWiaNxdcufcqg-Acuu-IO1-VjiOkyin1I0Ds4p_pHfS4lFl2wi94Tmc3_GMfzz6RyQkI2Gkf3wsGUI3NZU2W3laN-gK2BSnZLAooD1QdtNBfEP-fbp5fiHyl6siqtuID9aIVNrl80iJhqSunvQsYZzP1LZRRY2RSOHjiSRffs_3PCMWGRIxI97dX0NQwf-OaKcpHGYcr1l09z259Jg_9RtfKnfBZdAJkhsl72Tpk08UfL42hWu4AjLblUK_eg&clientId=tata-aig-a8b455"


//        val magicLink =
//            "https://star-health.getvisitapp.com/?mluib7c=QNkg98jB"


        IntiateSdk.s(
            this,
            false,
            magicLink,
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
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}

