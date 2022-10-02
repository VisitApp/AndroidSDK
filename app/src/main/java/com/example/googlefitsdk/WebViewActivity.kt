package com.example.googlefitsdk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.getvisitapp.google_fit.data.GoogleFitStatusListener
import com.getvisitapp.google_fit.data.GoogleFitUtil
import im.delight.android.webview.AdvancedWebView

class WebViewActivity : AppCompatActivity(), AdvancedWebView.Listener, GoogleFitStatusListener {

    var TAG = "mytag"

    lateinit var mWebView: AdvancedWebView
    val ACTIVITY_RECOGNITION_REQUEST_CODE = 490
    val LOCATION_PERMISSION_REQUEST_CODE = 787
    lateinit var googleFitUtil: GoogleFitUtil

    var default_web_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"
    var baseUrl = "https://star-health.getvisitapp.xyz/"

    var dailyDataSynced = false
    var syncDataWithServer = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        mWebView = findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setMixedContentAllowed(false);
        mWebView.settings.javaScriptEnabled = true

//        val magicLink =
//            "https://star-health.getvisitapp.xyz/star-health?token=eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOi[%E2%80%A6]GFsIn0.f0656mzmcRMSCywkbEptdd6JgkDfIqN0S9t-P1aPyt8&id=8158";
        val magicLink =
            "https://star-health.getvisitapp.xyz/login"
        mWebView.loadUrl(magicLink)

        googleFitUtil =
            GoogleFitUtil(this, this, default_web_client_id)
        mWebView.addJavascriptInterface(googleFitUtil.webAppInterface, "Android")
        googleFitUtil.init()


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
        Log.d(TAG, "onPageStarted: $url")
    }

    override fun onPageFinished(url: String?) {
        Log.d(TAG, "onPageFinished: $url")

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

        googleFitUtil.onActivityResult(requestCode, resultCode, intent)


        mWebView.onActivityResult(requestCode, resultCode, intent);

    }

    override fun onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return; }
        super.onBackPressed()

    }

    override fun askForPermissions() {
        googleFitUtil.askForGoogleFitPermission()
    }

    override fun onFitnessPermissionGranted() {
        Log.d(TAG, "onFitnessPermissionGranted() called")
        runOnUiThread(Runnable { googleFitUtil.fetchDataFromFit() })
    }

    override fun onFitnessPermissionCancelled() {
        Log.d(TAG, "onFitnessPermissionCancelled() called")
    }

    override fun onFitnessPermissionDenied() {
        Log.d(TAG, "onFitnessPermissionCancelled() called")
    }

    override fun loadDailyFitnessData(steps: Long, sleep: Long, calorie: Float) {
        Log.d("mytag", "daily Fitness Data url:$steps, sleep: $sleep")

    }

    override fun requestActivityData(type: String?, frequency: String?, timestamp: Long) {
        Log.d(TAG, "requestActivityData() called.")
        runOnUiThread(Runnable {
            if (type != null && frequency != null) {
                googleFitUtil.getActivityData(type, frequency, timestamp)
            }
        })
    }

    override fun loadGraphData(data: String?) {
        if (data != null) {
            mWebView.evaluateJavascript(
                data,
                null
            )
        }
        dailyDataSynced = true
    }

    override fun syncDataWithServer(
        baseUrl: String?,
        authToken: String?,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long
    ) {
        Log.d("mytag", "baseUrl: $baseUrl")
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called")
            runOnUiThread(Runnable {
                googleFitUtil.sendDataToServer(
                    baseUrl + "/",
                    authToken,
                    googleFitLastSync,
                    gfHourlyLastSync
                )
                syncDataWithServer = true
            })
        }
    }

    override fun askForLocationPermission() {
        if (dailyDataSynced) {
            return
        }
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

    override fun setDailyFitnessDataJSON(data: String?) {

    }

    override fun setHourlyFitnessDataJSON(data: String?) {

    }

    override fun setGoogleFitConnection(isGranted: Boolean) {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTIVITY_RECOGNITION_REQUEST_CODE -> {
                Log.d(TAG, "ACTIVITY_RECOGNITION_REQUEST_CODE permission granted")
                googleFitUtil.askForGoogleFitPermission()
            }
            LOCATION_PERMISSION_REQUEST_CODE -> {}
        }
    }


}

