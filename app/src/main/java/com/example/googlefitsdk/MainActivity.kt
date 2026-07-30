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

        VisitSDK.getUserEventCallback { eventName: String ->

            Timber.tag("mytag").d("setUserEventCallback eventName: $eventName")
        }

        VisitSDK.getErrorEventCallback { errorMessage: String, description: String? ->

            Timber.tag("mytag")
                .d("setErrorEventCallback errorMessage: $errorMessage, description: $description")
        }
    }


    private fun init() {

        val ssoLink =
            "https://niva-bupa-visit.getvisitapp.net/sso?userParams=23dunqltP9O2DMyulwb-QtVyWXud2_inHkw-IXo0LCTavm4b4dxrddNu9tREryI3nM8dMnea6jBLeRH6lHS3PRGoRozW2rpkPtcaemIaKh0RDKfi6uKjoKguyGHYrTvD7vQtEoE-iqwKWoHgbx5UW5YfB2HQrJwuSzW0yTt6g9tw0PLELX47Epjc17Ipw1MNWiwJ2Q1yWn2cxWWTIoYJuN4gBHDmEioS2bJyMQlvDDFoVjn4wzBbYnKD-UEi9zWX3Thy1n6bwicAnlDxh2jSbIH7b_E5z6g9SDBRzJnis9HyQZ4xmYG405WKkypEUWW6_KJRqCBDEJjt5iFAuzrO8cERpQmPO-nkhBJAgNpJ8VrCFhG_XIZX07oJKmuK5xTKsDQUV5Y9wya6nV0niYMq1KYcGABdm_qLY4IWXcmHO7KNi3LDcBZLuAT2QZikxQtqKAi6sxJ7d30vvXBmw6Isl8rvLUfsnPecqocncwl3YpOLKqVwoBTx_ONmFZeEr5GX&clientId=NIVA_BUPA"

        VisitSDK.init(
            this, false, ssoLink
        )
    }

}

