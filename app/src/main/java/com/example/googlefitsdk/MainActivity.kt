package com.example.googlefitsdk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.zomato.chatsdk.activities.ChatSDKDeepLinkRouter


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button).setOnClickListener {
            init()
        }
    }


    private fun init() {
        val intent = Intent(
            this,
            ChatSDKDeepLinkRouter::class.java
        )
        intent.putExtra("uri", "")
        startActivity(intent)
    }
}


