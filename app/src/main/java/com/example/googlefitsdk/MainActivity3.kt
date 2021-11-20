package com.example.googlefitsdk

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)

        val txtConfig = findViewById<TextView>(R.id.txtConfig)

        val configText = """
            BASE URL :  ${BuildConfig.APP_BASE_URL}
            MAP_KEY :  ${getString(R.string.maps_key)}
            RE-ATTEMPT :  ${BuildConfig.APP_RETRY_ATTEMPTS}
            THRESHOLD VALUE :  ${BuildConfig.APP_THRESHOLD_VALUE}
        """.trimIndent()

        txtConfig.text = configText





    }
}