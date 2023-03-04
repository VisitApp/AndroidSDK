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
            "https://tata-aig.getvisitapp.xyz/sso?userParams=yuAeVTpF4C3w2cguETyMeZZJBkZCkNt55RRYHIirGDLbzgtW0f4dfYKyUUxMzSaq0IYjOuyavj2nJvfPnyxFHzjmBIA2m2yrMIB2F5l-kO-MZgdl5afhShrepawOSwcavR-ctyzy82303U_FMACWbhEKNPe9hyYGjot8Db0yG9GwyGbtK8ej01NnabNPQ3uffi3vUZ-f1zaH1ub42m5gxISVTd7n3K-gBJF1F4EcaF7_98hzBfk-I9Zr-KZcdSXtt9ZV70IB-JbFSuauZtjLCl3NmGRprLeoYXd1QT3V0aQ&clientId=tata-aig-a8b455"

        IntiateSdk.s(
            this,
            false,
            magicLink,
            default_client_id
        )
    }




}

