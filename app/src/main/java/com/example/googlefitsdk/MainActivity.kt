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
            "https://navi-visit.getvisitapp.net/sso?userParams=rPjf1gmSCqbeDwqVKqRhpt-TVihQ_XmLWt99w4avNGe0-jO3ER8QHahQ-vho1kDYO0ysZqaZ4C06W2VYdht5wxbxoE8ULxgaxQKrTIX7TB4m6nbr94ALRMR3LHHNBSPtC1d56vYBiGUirYo8Ltvodjcl0UKWVcO1Ierp3uPRDLZUUTjzzRkAJYcwxK8FPs2x-LHG7-tXTM969K8Yw-nvz20kJmkdUcWz2jKUrsXX7vRC-iOnwE8SeDvh7G_2NI0XVQybbaAsCkjYAQFAX7j4p1-7i3Vc5Y0yttDhQPWZmEc=&clientId=navi-f3vkn"



        IntiateSdk.s(
            this,
            false,
            magicLink
        )
    }
}

