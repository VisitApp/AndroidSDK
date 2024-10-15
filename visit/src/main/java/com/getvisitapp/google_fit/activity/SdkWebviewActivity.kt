package com.getvisitapp.google_fit.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.connectivity.ConnectivityObserver
import com.getvisitapp.google_fit.connectivity.NetworkConnectivityObserver
import com.getvisitapp.google_fit.data.WebAppInterface
import com.getvisitapp.google_fit.databinding.SdkWebView
import com.getvisitapp.google_fit.util.Constants.IS_DEBUG
import com.getvisitapp.google_fit.util.Constants.WEB_URL
import com.getvisitapp.google_fit.util.LocationTrackerUtil
import com.getvisitapp.google_fit.util.PdfDownloader
import com.getvisitapp.google_fit.util.makeStatusBarTransparent
import com.getvisitapp.google_fit.view.GoogleFitStatusListener
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.json.JSONException
import org.json.JSONObject
import tvi.webrtc.ContextUtils
import java.util.*


/**
 *
 *
1. Fitbit success URL:
tataaig://visitsdkactivity&message=success&fitbit=true

2.
These events are there present in the sdk:

Download HRA report clicked (done)
Download HRA report failed (not possible from sdk)
Google fit clicked (done)
Google fit connection failed (done)
Fitbit clicked (done)
Fitbit connection failed (done)
Sync steps and calories api called (done)
Sync steps and calories api failed (done)

 */
@Keep
class SdkWebviewActivity : AppCompatActivity(), GoogleFitStatusListener {

    var TAG = "mytag"

    lateinit var binding: SdkWebView


    val ACTIVITY_RECOGNITION_REQUEST_CODE = 490
    val LOCATION_PERMISSION_REQUEST_CODE = 787
    val REQUEST_CODE_FILE_PICKER = 51426


    var isDebug: Boolean = false
    lateinit var magicLink: String
    lateinit var default_web_client_id: String


    var dailyDataSynced = false
    var syncDataWithServer = false


    lateinit var pdfDownloader: PdfDownloader


    var visitApiBaseUrl: String? = null
    var authtoken: String? = null
    var googleFitLastSync: Long = 0L
    var gfHourlyLastSync = 0L


    lateinit var locationTrackerUtil: LocationTrackerUtil
    private val AUTHORITY_SUFFIX = ".googlefitsdk.fileprovider"

    lateinit var connectivityObserver: ConnectivityObserver
    var mFileUploadCallbackSecond: ValueCallback<Array<Uri>>? = null


    var webChromeClient: WebChromeClient = MyChrome()
    var webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.d(TAG, "onPageStarted: $url")
            binding.progressBar.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d(TAG, "onPageFinished: $url")
            binding.progressBar.visibility = View.GONE

        }


        override fun onReceivedError(
            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
        ) {
//            Log.d(TAG, "errorCode: $errorCode description: $description failingUrl: $failingUrl")
            binding.progressBar.visibility = View.GONE
            Log.d("mytag", "onReceivedError")
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            Log.d("mytag", "shouldOverrideUrlLoading")

            url?.let {
                binding.webview.loadUrl(url)
            }
            return true;

        }
    }

    lateinit var webAppInterface: WebAppInterface


    companion object {
        fun getIntent(
            context: Context,
            isDebug: Boolean,
            magicLink: String,
        ): Intent {
            val intent = Intent(context, SdkWebviewActivity::class.java);
            intent.putExtra(IS_DEBUG, isDebug)
            intent.putExtra(WEB_URL, magicLink)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeStatusBarTransparent()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sdk)
        binding.progressBar.setVisibility(View.GONE)
        magicLink = intent.extras!!.getString(WEB_URL)!!
        isDebug = intent.extras!!.getBoolean(IS_DEBUG);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isDebug) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webview, true)
        binding.webview.settings.setGeolocationEnabled(true)
        binding.webview.settings.setDomStorageEnabled(true);
        binding.webview.settings.setCacheMode(WebSettings.LOAD_NO_CACHE)

        binding.webview.webChromeClient = webChromeClient
        binding.webview.webViewClient = webViewClient

        binding.webview.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String?,
                userAgent: String?,
                contentDisposition: String?,
                mimetype: String?,
                contentLength: Long
            ) {

                Log.d("mytag", "onDownloadRequested() url:$url, mimeType:$mimetype");

                url?.let {
                    pdfDownloader.downloadPdfFile(fileDir = filesDir,
                        pdfUrl = url,
                        onDownloadComplete = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                        applicationContext,
                                        applicationContext.packageName + AUTHORITY_SUFFIX,
                                        it
                                    )
                                )
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                type = "application/pdf"
                            }
                            val sendIntent = Intent.createChooser(shareIntent, null)
                            startActivity(sendIntent)
                        },
                        onDownloadFailed = {
                            Log.d(
                                TAG, "onDownloadRequested() download failed, opening it in chrome"
                            )
                            try {
                                val uri = Uri.parse(url)
                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        })
                }

            }
        })

        binding.webview.loadUrl(magicLink)


        webAppInterface = WebAppInterface(this)
        binding.webview.addJavascriptInterface(webAppInterface, "Android")

        pdfDownloader = PdfDownloader()
        locationTrackerUtil = LocationTrackerUtil(this)

        connectivityObserver = NetworkConnectivityObserver(this)

        connectivityObserver.observe().onEach { networkStatus ->
            when (networkStatus) {
                ConnectivityObserver.Status.Available -> {
                    binding.noNetworkConnectionLayout.visibility = View.GONE
                }

                ConnectivityObserver.Status.Unavailable -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Losing -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Lost -> {
                    binding.noNetworkConnectionLayout.visibility = View.VISIBLE
                }
            }
            Log.d("mytag", "network status: $networkStatus")
        }.launchIn(lifecycleScope)


    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume();
    }

    override fun onPause() {
        binding.webview.onPause();
        super.onPause()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Log.d(
            TAG, "onActivityResult called. requestCode: $requestCode resultCode: $resultCode"
        )

        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            Log.d("mytag", "resultCode: $requestCode")

            binding.webview.webChromeClient = webChromeClient
            binding.webview.webViewClient = webViewClient


        } else if (requestCode == REQUEST_CODE_FILE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                var dataUris: Array<Uri>? = null

                try {
                    if (intent!!.dataString != null) {
                        dataUris = arrayOf(Uri.parse(intent.dataString))
                    } else {
                        if (intent.clipData != null) {
                            val count = intent.clipData!!.itemCount
                            dataUris = Array(count) { index ->
                                intent.clipData!!.getItemAt(index).uri
                            }
                        }
                    }
                } catch (ignored: java.lang.Exception) {

                }

                mFileUploadCallbackSecond!!.onReceiveValue(dataUris)
                mFileUploadCallbackSecond = null
            } else {
                if (mFileUploadCallbackSecond != null) {
                    mFileUploadCallbackSecond!!.onReceiveValue(null)
                    mFileUploadCallbackSecond = null
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun askForLocationPermission() {

        runOnUiThread {
            if (locationTrackerUtil.isLocationPermissionAllowed()) {
                if (locationTrackerUtil.isGPSEnabled()) {
                    runOnUiThread {
                        binding.webview.evaluateJavascript(
                            "window.checkTheGpsPermission(true)", null
                        )
                        Log.d("mytag", "window.checkTheGpsPermission(true) called")
                    }
                } else {
                    locationTrackerUtil.showGPS_NotEnabledDialog()
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onRestart() {
        super.onRestart()
        if (locationTrackerUtil.isLocationPermissionAllowed() && locationTrackerUtil.isGPSEnabled()) {
            runOnUiThread {
                binding.webview.evaluateJavascript(
                    "window.checkTheGpsPermission(true)", null
                )
                Log.d("mytag", "window.checkTheGpsPermission(true) called")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val locationPermissionGranted =
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)

                    if (locationPermissionGranted) {
                        if (!locationTrackerUtil.isGPSEnabled()) {
                            locationTrackerUtil.showGPS_NotEnabledDialog()
                        } else {
                            runOnUiThread {
                                binding.webview.evaluateJavascript(
                                    "window.checkTheGpsPermission(true)", null
                                )
                                Log.d("mytag", "window.checkTheGpsPermission(true) called")
                            }
                        }
                    } else {
                        locationTrackerUtil.showLocationPermissionDeniedAlertDialog()
                    }
                }
            }
        }
    }

    override fun startVideoCall(sessionId: Int, consultationId: Int, authToken: String?) {


        val intent = Intent(
            this, TwillioVideoCallActivity::class.java
        )
        intent.putExtra("isDebug", isDebug)
        intent.putExtra("sessionId", sessionId)
        intent.putExtra("consultationId", consultationId)
        intent.putExtra("authToken", authToken)
        startActivity(intent)
    }

    override fun closeView() {
        finish()
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    Log.d(TAG, "webview.canGoBack(): ${binding.webview.canGoBack()}")

                    Log.d(TAG, binding.webview.url.toString())

                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                        if (binding.webview.url!!.endsWith("consultation/online/preview")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/weight-management")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("op-benefits")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/home/rewards")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/wellness-management")) {
                            finish()
                        } else if (binding.webview.url!!.endsWith("/health-data") || binding.webview.url!!.endsWith(
                                "/hra/question"
                            ) || binding.webview.url!!.contains("stay-active")
                        ) {
                            Log.d(TAG, "window.hardwareBackPressed() called")
                            runOnUiThread {
                                binding.webview.evaluateJavascript(
                                    "window.hardwareBackPressed()", null
                                ) // this is a workaround to close the PWA, when the user lands to the details graph page directly,
                                // so this event acts as a gateway to check if the user has directly landed on the page, and close the PWA.
                            }
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

    fun closePWA() {
        finish()
        overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        super.onDestroy()
    }


    override fun openLink(url: String?) {
        runOnUiThread {
            try {
                val uri = Uri.parse(url)
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    inner class MyChrome internal constructor() : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0
        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(
                ContextUtils.getApplicationContext().resources, 2130837573
            )
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (mFileUploadCallbackSecond != null) {
                mFileUploadCallbackSecond!!.onReceiveValue(null)
            }
            mFileUploadCallbackSecond = filePathCallback

            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)

            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            i.type = "*/*"

            startActivityForResult(
                Intent.createChooser(i, "Choose a file"), REQUEST_CODE_FILE_PICKER
            )

            return true

        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(
            paramView: View, paramCustomViewCallback: CustomViewCallback
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.getSystemUiVisibility()
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(
                mCustomView, FrameLayout.LayoutParams(-1, -1)
            )
            window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            Log.d("mytag", "onGeolocationPermissionsShowPrompt called")
            super.onGeolocationPermissionsShowPrompt(origin, callback);
            callback?.invoke(origin, true, false);
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Log.d("mytag", "onGeolocationPermissionsHidePrompt called")
            super.onGeolocationPermissionsHidePrompt()

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webview.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webview.restoreState(savedInstanceState)
    }

    @Throws(JSONException::class)
    private fun decodeString(response: String): JSONObject {
        return if (Build.VERSION.SDK_INT >= 24) {
            JSONObject(Html.fromHtml(response, Html.FROM_HTML_MODE_LEGACY).toString())
        } else {
            JSONObject(Html.fromHtml(response).toString())
        }
    }


}




