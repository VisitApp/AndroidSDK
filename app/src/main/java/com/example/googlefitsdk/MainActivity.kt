package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.IntiateSdk


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

    }


    fun init() {

        val magicLink =
            "https://navi-visit.getvisitapp.com/sso?userParams=e-GE4kSPM_9k6QQqMI4QQnlloLJQg15QujHKvLvRlYsvquGpBPIkqmcLN2FnLvABtChG-ofoUICopmOJ5GaPipHNFzZeog8LhKt0T8QF4qrJXMnoPqTEtSq90vxJOW3_qEIUrpR5PgmPfzy3IUVRjERjW8zOxFcwHXs-WWSxDO5s51TSEfr8ZBwQcsO93segMhOrCgBKf2sBkppix1RTL775fNHbsugANiT-weICAvYVP9V69VDqo51yZq64mfNS&clientId=navi-f3vkn"



        IntiateSdk.s(
            this,
            false,
            magicLink
        )
    }
}

