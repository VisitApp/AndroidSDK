package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.getvisitapp.visit.VisitSDK
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var ssoLinkEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ssoLinkEditText = findViewById(R.id.ssoLinkEditText)

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

        val ssoLink = ssoLinkEditText.text.toString().trim()
        if (ssoLink.isBlank()) {
            ssoLinkEditText.error = "SSO link is required"
            return
        }

        VisitSDK.init(
            this, false, ssoLink
        )
    }

}
