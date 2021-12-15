package com.getvisitapp.google_fit.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.data.GoogleFitStatusListener
import com.getvisitapp.google_fit.data.GoogleFitUtil
import com.getvisitapp.google_fit.databinding.SdkWebView
import com.getvisitapp.google_fit.util.Constants.BASE_URL
import com.getvisitapp.google_fit.util.Constants.DEFAULT_CLIENT_ID
import com.getvisitapp.google_fit.util.Constants.IS_DEBUG
import com.getvisitapp.google_fit.util.Constants.WEB_URL
import im.delight.android.webview.AdvancedWebView

class SdkWebviewActivity : AppCompatActivity(), AdvancedWebView.Listener,
    VideoCallListener, GoogleFitStatusListener {

    var TAG = "mytag"

    lateinit var binding: SdkWebView


    val ACTIVITY_RECOGNITION_REQUEST_CODE = 490
    val LOCATION_PERMISSION_REQUEST_CODE = 787
    lateinit var googleFitUtil: GoogleFitUtil

    var isDebug: Boolean = false
    lateinit var magicLink: String
    lateinit var default_web_client_id:String
    lateinit var baseUrl: String


    var dailyDataSynced = false
    var syncDataWithServer = false

    companion object {
        fun getIntent(
            context: Context,
            isDebug: Boolean,
            magicLink: String,
            baseUrl: String,
            default_web_client_id: String
        ): Intent {
            val intent = Intent(context, SdkWebviewActivity::class.java);
            intent.putExtra(IS_DEBUG, isDebug)
            intent.putExtra(WEB_URL, magicLink)
            intent.putExtra(BASE_URL, baseUrl)
            intent.putExtra(DEFAULT_CLIENT_ID, default_web_client_id)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sdk)
        binding.infoView.setVisibility(View.GONE)
        magicLink = intent.extras!!.getString(WEB_URL)!!
        baseUrl =
            intent.extras!!.getString(BASE_URL)!!  //the need of baseUrl if only for updating the google fit daily steps card after the webpage as loaded.
        isDebug = intent.extras!!.getBoolean(IS_DEBUG);
        default_web_client_id = intent.extras!!.getString(DEFAULT_CLIENT_ID)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        binding.webview.setListener(this, this)
        binding.webview.setGeolocationEnabled(true)
        binding.webview.setMixedContentAllowed(true)
        binding.webview.setCookiesEnabled(true)
        binding.webview.setThirdPartyCookiesEnabled(true)

        binding.webview.loadUrl(magicLink)


        googleFitUtil =
            GoogleFitUtil(this, this, default_web_client_id, baseUrl)
        binding.webview.addJavascriptInterface(googleFitUtil.webAppInterface, "Android")
        googleFitUtil.init()

    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume();
    }

    override fun onPause() {
        binding.webview.onPause();
        super.onPause()
    }

    override fun onDestroy() {
        binding.webview.onDestroy();
        super.onDestroy()
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        Log.d(TAG, "onPageStarted: $url")
        binding.infoView.visibility = View.VISIBLE
    }

    override fun onPageFinished(url: String?) {
        Log.d(TAG, "onPageFinished: $url")
        binding.infoView.visibility = View.GONE
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {
        Log.d(TAG, "errorCode: $errorCode description: $description failingUrl: $failingUrl")
        binding.infoView.visibility = View.GONE
    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {

        try {
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))

        } catch (e: Exception) {

        }

    }

    override fun onExternalPageRequest(url: String?) {
        try {
            val uri = Uri.parse(url)
            startActivity(Intent(Intent.ACTION_VIEW, uri))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.d(
            TAG,
            "onActivityResult called. requestCode: $requestCode resultCode: $resultCode"
        )

        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == 4097 || requestCode == 1900) {
            googleFitUtil.onActivityResult(requestCode, resultCode, intent)
        } else {
            binding.webview.onActivityResult(requestCode, resultCode, intent);
        }
    }

    override fun askForPermissions() {
        if (dailyDataSynced) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    ACTIVITY_RECOGNITION_REQUEST_CODE
                )
            } else {
                googleFitUtil.askForGoogleFitPermission()
            }
        }
    }

    override fun onFitnessPermissionGranted() {
        Log.d(TAG, "onFitnessPermissionGranted() called")
        runOnUiThread(Runnable { googleFitUtil.fetchDataFromFit() })
    }

    override fun loadWebUrl(urlString: String?) {
        Log.d("mytag", "daily Fitness Data url:$urlString")
        if (urlString != null) {
            binding.webview.loadUrl(urlString)
        }
    }

    override fun requestActivityData(type: String?, frequency: String?, timestamp: Long) {
        Log.d(TAG, "requestActivityData() called.")
        runOnUiThread(Runnable {
            if (type != null && frequency != null) {
                googleFitUtil.getActivityData(type, frequency, timestamp)
            }
        })
    }

    override fun loadGraphDataUrl(url: String?) {
        if (url != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                binding.webview.evaluateJavascript(
                    url,
                    null
                )
            }
        }
        dailyDataSynced = true
    }

    override fun syncDataWithServer(
        apiBaseUrl: String?,
        authtoken: String?,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long
    ) {
        Log.d("mytag", "baseUrl: $baseUrl")
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called")
            runOnUiThread(Runnable {
                googleFitUtil.sendDataToServer(
                    baseUrl + "/",
                    authtoken,
                    googleFitLastSync,
                    gfHourlyLastSync
                )
                syncDataWithServer = true
            })
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

    override fun startVideoCall(sessionId: Int, consultationId: Int, authToken: String?) {

        val intent = Intent(
            this,
            TwillioVideoCallActivity::class.java
        )
        intent.putExtra("isDebug", isDebug)
        intent.putExtra("sessionId", sessionId)
        intent.putExtra("consultationId", consultationId)
        intent.putExtra("authToken", authToken)
        startActivity(intent)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                        Log.d("Webview Url", binding.webview.url.toString())
                        if (binding.webview.url!!.contains("home")) {
                            finish()
                        }
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


}