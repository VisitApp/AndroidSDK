package com.example.googlefitsdk

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.getvisitapp.visit.VisitSDK


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }
    }


    private fun init() {

        val magicLink =
            "https://retail-stage.getvisitapp.net/ultron/scan-and-pay/receive-direct-bank-payment?orderId=5383&userId=42&phone=7411260996&amount=12"

        val token =
            "JWT eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjQyLCJwbGF0Zm9ybSI6IkFORFJPSUQiLCJ1c2VyVHlwZSI6InVzZXIiLCJpYXQiOjE3MzMzMDAzNzUsImV4cCI6MTc2NDgzNjM3NX0.i3SoCgdobic3HMI9sH5kaK9GgDdCZR6AI7oIlSq3NF0"

        VisitSDK.init(
            this, magicLink, token
        )
    }

}

