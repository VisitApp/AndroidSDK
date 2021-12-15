package com.example.googlefitsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
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
        //        val prodMagicLink = "https://vsyt.me/m/1XFjA45h"
        val prodLink = "https://web.getvisitapp.com/v4/"
//        val debugLink = "https://care.getvisitapp.xyz/"
        val magicLink =
            " https://star-health.getvisitapp.xyz/star-health?token=eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOi[%E2%80%A6]GFsIn0.f0656mzmcRMSCywkbEptdd6JgkDfIqN0S9t-P1aPyt8&id=8158"
        val baseUrlOfMagicLink =
            "https://star-health.getvisitapp.xyz/" //the need of baseUrl if only for updating the google fit daily steps card after the webpage as loaded.
        val default_client_id =
            "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"


        IntiateSdk.s(this, false, magicLink, baseUrlOfMagicLink, default_client_id)
    }
}