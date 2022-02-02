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
        //        val prodMagicLink = "https://vsyt.me/m/1XFjA45h"
//        val prodLinkMagicLink = "https://web.getvisitapp.com"
//        val prodBaseUrl = "https://web.getvisitapp.com/"

        val prodLinkMagicLink = "https://tata-aig.getvisitapp.xyz"
        val prodBaseUrl = "https://tata-aig.getvisitapp.xyz/"



        val default_client_id =
            "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"


        IntiateSdk.s(this, false, prodLinkMagicLink, prodBaseUrl, default_client_id)
    }
}