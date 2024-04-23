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
            "https://tata-aig.getvisitapp.xyz/sso?userParams=LH53MbTMvD-UcbZxap-i6S-h-D2Y0JgBpwJdDPhiJGjGsqYHDbZ0C3hZruy4_7BcCkS5elSq_4Xc3oIaieYs7pvT5dWiaNxdcufcqg-Acuu-IO1-VjiOkyin1I0Ds4p_pHfS4lFl2wi94Tmc3_GMfzz6RyQkI2Gkf3wsGUI3NZU2W3laN-gK2BSnZLAooD1QdtNBfEP-fbp5fiHyl6siqtuID9aIVNrl80iJhqSunvQsYZzP1LZRRY2RSOHjiSRffs_3PCMWGRIxI97dX0NQwf-OaKcpHGYcr1l09z259Jg_9RtfKnfBZdAJkhsl72Tpk08UfL42hWu4AjLblUK_eg&clientId=tata-aig-a8b455"


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

