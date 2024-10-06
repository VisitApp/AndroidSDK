package com.getvisitapp.google_fit.healthConnect.activity

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.R

class ViewPermissionUsageActivity : AppCompatActivity() {

    lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_permission_usage)

        webView = findViewById(R.id.webView)

        val privacyPolicyLink = "https://www.iffcotokio.co.in/about-us/privacy-policy"

        val settings: WebSettings = webView.getSettings()
        settings.setJavaScriptEnabled(true)
        webView.loadUrl(privacyPolicyLink)

    }
}