package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

//        val magicLink =
//            "https://abhi-sdk.getvisitapp.com/sso?userParams=8d6wbkkDSHcf1oQG3GPuE1a1GQ4VDObte-tpi3wI1xVwT2mOttZ2cWY1fSV86FvPz_axlxlhx0lZD2PoE3xpzUBEnfNSN0NAc6AwsukBSJQAhRYJxxTjrBFy6re-PfqOgJLEetNtj4EGguWuqgpHjeaOlmss1TkAGSBlTfgFYOd32IzzDYItZXwpv9BNOjBVhQQn2fhUkyOvaXmg9WsIxQ_8EmzaVwaMw0s_J_GHCFOu2lN8ea9j3xBC9SbEWKDZwXNnd63xHU8GQ4luzVrqNx8IGkUz6bS-FbR_9czjQl-MT2icWSUa8_hQSp41hobn&clientId=abhi-58fd14"


//        val magicLink =
//            "https://star-health.getvisitapp.com/?mluib7c=QNkg98jB"


        IntiateSdk.s(
            this,
            false,
            findViewById<EditText>(R.id.editTextText).text.toString(),
            default_client_id
        )
    }
}

