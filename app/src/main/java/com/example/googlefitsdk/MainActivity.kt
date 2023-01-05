package com.example.googlefitsdk

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.IntiateSdk
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.data.VisitStepSyncHelper.Companion.openGoogleFit
import com.getvisitapp.google_fit.event.ClosePWAEvent
import com.getvisitapp.google_fit.event.MessageEvent
import com.getvisitapp.google_fit.event.VisitEventType
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    var TAG = "mytag10"

    val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"

    lateinit var checker: GoogleFitAccessChecker
    lateinit var switch: Switch

    val tataAIG_base_url = "https://uathealthvas.tataaig.com"
    val tataAIG_auth_token = "Basic Z2V0X3Zpc2l0OkZoNjh2JHdqaHU4WWd3NiQ="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }


        switch = findViewById<Switch>(R.id.switch1)

        checker = GoogleFitAccessChecker(this)

        val syncStepHelper = VisitStepSyncHelper(context = this, default_client_id)
        syncStepHelper.syncSteps(tataAIG_base_url, tataAIG_auth_token)


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
                VisitEventType.FitnessPermissionGranted -> {


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
                VisitEventType.ConsultationBooked ->{
                    Log.d("mytag","MainActivity ConsultationBooked event")
                }
            }

        }

    }

    fun init() {
//        val magicLink = "https://tata-aig.getvisitapp.xyz"
//        val magicLink ="https://tata-aig.getvisitapp.xyz/weight-management"
//        val magicLink ="https://star-health.getvisitapp.xyz/?mluib7c=0ZidhSZ1"
//        val magicLink ="https://tata-aig.getvisitapp.xyz/sso?userParams=yuAeVTpF4C3w2cguETyMeZZJBkZCkNt55RRYHIirGDLbzgtW0f4dfYKyUUxMzSaq0IYjOuyavj2nJvfPnyxFHzjmBIA2m2yrMIB2F5l-kO-MZgdl5afhShrepawOSwcavR-ctyzy82303U_FMACWbhEKNPe9hyYGjot8Db0yG9GwyGbtK8ej01NnabNPQ3uffi3vUZ-f1zaH1ub42m5gxISVTd7n3K-gBJF1F4EcaF7_98hzBfk-I9Zr-KZcdSXtt9ZV70IB-JbFSuauZtjLCl3NmGRprLeoYXd1QT3V0aQ&clientId=tata-aig-a8b455"
        val magicLink ="https://tata-aig.getvisitapp.xyz/sso?userParams=Kzdzgp8Pe6DTEgvljdI5itsRV03uMwm5wlrnRHA9jsxtfdU8A2FfhAcno_A3ja8faMn2B2HQRAkQ3i5v14WLbovrEg_9sgoluwcs-4pmtcxQxk61NhS1hR1y0lEdwbnrTrmWdGGqpDDoln-gxsEB6Mk3FYprNuE-SjwGnzlacxhnD3s8In7haPYJXMteENBvyDLKG_O4iR2n7Bhz08GM_EcsFRWWVLc_NHF-XvG_OtgldLTsrsQDWuBdkT9fCXl1nfhN1EfbcrbjVnLCHQNEwZkSS56F7jbeRAEyjkzuZzA&clientId=tata-aig-a8b455"
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
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checker.checkGoogleFitAccess()
        switch.setOnCheckedChangeListener(this)
    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (isChecked) {
            init()
        } else {
            checker.revokeGoogleFitPermission(default_client_id)
        }
    }


    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}

