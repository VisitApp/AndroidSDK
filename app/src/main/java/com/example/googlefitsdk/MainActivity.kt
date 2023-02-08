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


class MainActivity : AppCompatActivity() {
    var TAG = "mytag10"

    val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

    }

    fun init() {

        val magicLink =
            "https://api.samuraijack.xyz/url-shortener/m/JAjYyb70"

        IntiateSdk.s(
            this,
            false,
            magicLink,
            default_client_id
        )
    }




}

