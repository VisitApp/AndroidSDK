package com.example.googlefitsdk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.getvisitapp.google_fit.HealthConnectListener
import com.getvisitapp.google_fit.data.GoogleFitStatusListener
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.data.WebAppInterface
import com.getvisitapp.google_fit.healthConnect.activity.HealthConnectUtil
import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
import im.delight.android.webview.AdvancedWebView
import timber.log.Timber

class WebViewActivity : AppCompatActivity(), AdvancedWebView.Listener, GoogleFitStatusListener,
    HealthConnectListener {

    var TAG = "mytag"

    lateinit var mWebView: AdvancedWebView

    private val LOCATION_PERMISSION_REQUEST_CODE = 787

    lateinit var visitStepSyncHelper: VisitStepSyncHelper

    lateinit var healthConnectUtil: HealthConnectUtil

    var syncDataWithServer = false

    lateinit var webAppInterface: WebAppInterface


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)


        //this code will not be shipped in react native webview.
        mWebView = findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setMixedContentAllowed(false);
        mWebView.settings.javaScriptEnabled = true
        webAppInterface = WebAppInterface(this)

        val magicLink =
            "https://star-health.getvisitapp.com/?mluib7c=wx6hHGGG"

        ActivityCompat.requestPermissions(
            this, arrayOf<String>(
                Manifest.permission.POST_NOTIFICATIONS
            ), 101
        )





        visitStepSyncHelper = VisitStepSyncHelper(this)
        healthConnectUtil = HealthConnectUtil(this, this)





        mWebView.loadUrl(magicLink)
        mWebView.addJavascriptInterface(webAppInterface, "Android")

        healthConnectUtil.initialize()


    }

    override fun onResume() {
        super.onResume()
        mWebView.onResume();
    }

    override fun onPause() {
        mWebView.onPause();
        super.onPause()
    }

    override fun onDestroy() {
        mWebView.onDestroy();
        super.onDestroy()
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        Timber.d("mytag: onPageStarted: $url")
    }

    override fun onPageFinished(url: String?) {
        Timber.d("mytag: onPageFinished: $url")

    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {}

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {
    }

    override fun onExternalPageRequest(url: String?) {}

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        mWebView.onActivityResult(requestCode, resultCode, intent);

    }

    override fun onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return; }
        super.onBackPressed()

    }

    override fun askForPermissions() {
        if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
            healthConnectUtil.getVisitDashboardGraph()
        } else {
            healthConnectUtil.requestPermission()
        }
    }

    override fun requestActivityData(type: String?, frequency: String?, timestamp: Long) {
        Timber.d("mytag: requestActivityData() called.")

        //Health Connect Implementation
        healthConnectUtil.getActivityData(type, frequency, timestamp)
    }

    override fun syncDataWithServer(
        baseUrl: String?,
        authToken: String?,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long
    ) {
        Timber.d("mytag: baseUrl: $baseUrl")
        if (!syncDataWithServer) {
            Timber.d("mytag: syncDataWithServer() called")

            visitStepSyncHelper.sendDataToVisitServer(
                healthConnectUtil,
                googleFitLastSync,
                gfHourlyLastSync,
                "$baseUrl/",
                authToken!!
            )

            syncDataWithServer = true
        }
    }

    override fun askForLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun closeVisitPWA() {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            LOCATION_PERMISSION_REQUEST_CODE -> {}
        }
    }

    override fun updateHealthConnectConnectionStatus(
        status: HealthConnectConnectionState,
        text: String
    ) {
        Timber.d("updateHealthConnectConnectionStatus: $status")

        when (status) {
            HealthConnectConnectionState.CONNECTED -> {
                healthConnectUtil.getVisitDashboardGraph()
            }

            HealthConnectConnectionState.NOT_SUPPORTED -> {

            }

            HealthConnectConnectionState.NOT_INSTALLED -> {

            }

            HealthConnectConnectionState.INSTALLED -> {
                //don't do anything here for the webView.
            }

            HealthConnectConnectionState.NONE -> {

            }
        }
    }

    //This handles both dashboard graph and detailed graph page.
    override fun loadVisitWebViewGraphData(webUrl: String) {
        Timber.d("loadVisitWebViewGraphData: webUrl: $webUrl")

        Handler(Looper.getMainLooper()).post {
            mWebView.evaluateJavascript(
                webUrl,
                null
            )
        }

    }

    override fun userDeniedHealthConnectPermission() {
        //write the promise here.

        Timber.d("mytag : userDeniedHealthConnectPermission")
    }

    override fun userAcceptedHealthConnectPermission() {
        //write the promise here.

        Timber.d("mytag : userAcceptedHealthConnectPermission")

    }


}

