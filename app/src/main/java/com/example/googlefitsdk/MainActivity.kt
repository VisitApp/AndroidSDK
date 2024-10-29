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

        VisitSDK.setUserEventCallback { eventName: String ->

            Timber.tag("mytag").d("setUserEventCallback eventName: $eventName")
        }

        VisitSDK.setErrorEventCallback { errorMessage: String, description: String? ->

            Timber.tag("mytag")
                .d("setErrorEventCallback errorMessage: $errorMessage, description: $description")
        }
    }


    private fun init() {

        val magicLink =
            "https://navi-visit.getvisitapp.net/sso?userParams=rPjf1gmSCqbeDwqVKqRhpt-TVihQ_XmLWt99w4avNGe0-jO3ER8QHahQ-vho1kDYO0ysZqaZ4C06W2VYdht5wxbxoE8ULxgaxQKrTIX7TB4m6nbr94ALRMR3LHHNBSPtC1d56vYBiGUirYo8Ltvodjcl0UKWVcO1Ierp3uPRDLZUUTjzzRkAJYcwxK8FPs2x-LHG7-tXTM969K8Yw-nvz20kJmkdUcWz2jKUrsXX7vRC-iOnwE8SeDvh7G_2NI0XVQybbaAsCkjYAQFAX7j4p1-7i3Vc5Y0yttDhQPWZmEc=&clientId=navi-f3vkn"

//        val magicLink =
//            "https://navi-visit.getvisitapp.com/sso?userParams=wy1KH07IEsrZLP4z_8Fh14w1smgCYzeFl4V1C8JXkXb6DylNoRj-WnE5H53E3qb41_jBFUtWafh6hZZxN4MgeFxN_sce_Aw9NVXEeBt3sG3EO1dhtOVdgpLfLfI4wRUXG4X0zSnwy-zng1WWHaiQEhvloV5gxlppiEWWtra9_l5JxJIKVA_RSYrUs3e2HTLkzygjayPyYZ9PhpGJ2zEpl-D8mXsqhTxk4O6crcu1A5mbw68-J0QJKiOTXGwm3bUq3fCI15rAmqsQGN1LREfS6yCFKhEcMc6V1h233yPhub_BV62L9V_4LBYitr4vFRvn-rlLd02LdI4Ny2i8Un8zqQ==&clientId=navi-f3vkn"
//
        VisitSDK.init(
            this, false, magicLink
        )


    }

}

