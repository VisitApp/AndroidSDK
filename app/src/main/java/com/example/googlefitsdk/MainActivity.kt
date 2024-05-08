package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.IntiateSdk


class MainActivity : AppCompatActivity() {
    private var TAG = "mytag10"

    private val default_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

    }



    fun init() {

//        val magicLink =
//            "https://web.getvisitapp.xyz/"

        val magicLink =
            "https://abhi.getvisitapp.net/sso?userParams=t1Q8E-aBzmU2_Y7xi17gjrMzTGsvH32N9GeePukIc-KjG6bmMrfsLpmrzpqNiKFkgfSB3NMF0SPeQMXITQ6QXBLpUzc7fLrgsdyF8MOw46_02_YH7ogZH_oekVYByGaQ-qSIyP3P8aCxGq5tIZ7QRxImqGtQxeV4pJXJCddGihL7-eIbe5ivM-cNQMM3iHkoNGR7ximmaCOK6iXVQVfxLMyuSFL5O7VCild5iphHX1s&clientId=abhi-58fd14"


//        val magicLink =
//            "https://star-health.getvisitapp.com/?mluib7c=QNkg98jB"


        IntiateSdk.s(
            this,
            false,
            magicLink,
            default_client_id
        )
    }
}

