package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.getvisitapp.visit.VisitSDK
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }

        VisitSDK.getUserEventCallback { eventName: String ->

            Timber.tag("mytag").d("setUserEventCallback eventName: $eventName")
        }

        VisitSDK.getErrorEventCallback { errorMessage: String, description: String? ->

            Timber.tag("mytag")
                .d("setErrorEventCallback errorMessage: $errorMessage, description: $description")
        }
    }


    private fun init() {

        val ssoLink =
            "https://web.getvisitapp.net"

        VisitSDK.init(
            this, false, ssoLink
        )
    }

}

