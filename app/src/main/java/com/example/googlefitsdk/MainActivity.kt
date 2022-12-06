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
            "https://api.getvisitapp.xyz/care/partners/consult?data=f9af%2Fcwc5hB4oUeJjiqdiqrTKDzIblG7oX2pL6Ql5I2Iz5D2Mt5W7EZLx%2F7AGyzTyQ2JPvBzH%2BJDNZn7JwAqHAgJZ3%2FyidaK2ojJfT5AdXY33aN1R%2F8xBv9waA7kE09KFTwD53bQyaPR0Et7osINIxYjH1TPo6gbNCRcGpR5a6sHa4hTWK2NJsrhZaHJzNUp9fZmc2MUph71aKeyZZIdewFtZQ4JwkA9%2BlvQCujDamFw9Nzf%2FFTx2W5ZNapER%2Bo7"

        IntiateSdk.s(
            this,
            false,
            magicLink,
            default_client_id
        )
    }




}

