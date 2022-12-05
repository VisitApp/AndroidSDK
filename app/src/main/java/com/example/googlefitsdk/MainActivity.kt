package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.IntiateSdk
import com.getvisitapp.google_fit.data.VisitStepSyncHelper.Companion.openGoogleFit
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker


class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    var TAG = "mytag10"

    val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"

    lateinit var checker: GoogleFitAccessChecker
    lateinit var switch: Switch


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

        switch = findViewById<Switch>(R.id.switch1)

        checker = GoogleFitAccessChecker(this)

        //Open Google Fit if installed else return false.
        findViewById<Button>(R.id.openGoogleFitApp).setOnClickListener {
            val result = openGoogleFit()
            if (result) {
                //do nothing
            } else {
                Toast.makeText(this, "Google Fit app is not installed.", Toast.LENGTH_SHORT).show()
            }
        }

    }


    fun init() {

        val magicLink =
            "https://tata-aig.getvisitapp.xyz/sso?userParams=yuAeVTpF4C3w2cguETyMeZZJBkZCkNt55RRYHIirGDLbzgtW0f4dfYKyUUxMzSaq0IYjOuyavj2nJvfPnyxFHzjmBIA2m2yrMIB2F5l-kO-MZgdl5afhShrepawOSwcavR-ctyzy82303U_FMACWbhEKNPe9hyYGjot8Db0yG9FcM3nQvrMAJqKIAwIfJoSeg8x8yYN6hXdwIYwcDsHfvRSEvU3fd7PKzVHMphgdQJdpfDwfK-zWUpRrmBVpPDrO7TuflUrTzTp-cvA8P-EtwTHkLUPTmlLu_LPBtrqJYBA&clientId=tata-aig-a8b455"

        IntiateSdk.s(
            this,
            false,
            magicLink,
            default_client_id
        )
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

}

