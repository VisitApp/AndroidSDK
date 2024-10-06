package com.getvisitapp.google_fit

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.health.connect.client.PermissionController
import com.getvisitapp.google_fit.data.GoogleFitStatusListener
import com.getvisitapp.google_fit.data.VisitStepSyncHelper
import com.getvisitapp.google_fit.data.WebAppInterface
import com.getvisitapp.google_fit.healthConnect.activity.HealthConnectUtil
import com.getvisitapp.google_fit.healthConnect.contants.Contants
import com.getvisitapp.google_fit.healthConnect.enums.HealthConnectConnectionState
import kotlinx.coroutines.launch
import timber.log.Timber

class CordovaFitnessActivity : AppCompatActivity(), GoogleFitStatusListener, HealthConnectListener {

    private val TAG: String = "mytag"

    private val FILECHOOSER_REQUESTCODE = 1


    private val LOCATION_PERMISSION: String = Manifest.permission.ACCESS_FINE_LOCATION

    private val LOCATION_PERMISSION_REQUEST_CODE: Int = 787

    private var mUploadCallback: ValueCallback<Array<Uri>>? = null
    private var syncDataWithServer = false

    private lateinit var webView: WebView


    lateinit var visitStepSyncHelper: VisitStepSyncHelper

    lateinit var healthConnectUtil: HealthConnectUtil
    lateinit var webAppInterface: WebAppInterface

    private val requestPermissionActivityContract: ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    val requestPermissions =
        registerForActivityResult(requestPermissionActivityContract) { granted: Set<String> ->
            if (granted.containsAll(healthConnectUtil.PERMISSIONS)) {
                Contants.previouslyRevoked = false

                Timber.d("Permissions successfully granted")
                healthConnectUtil.scope.launch {
                    healthConnectUtil.checkPermissionsAndRun(afterRequestingPermission = true)
                }

            } else {
                Timber.d(" Lack of required permissions")

                //Currently the Health Connect SDK, only asks for the remaining permission was the NOT granted in the first time, and when it return,
                //it also send the granted permission (and not the permission that was previously granted), so the control flow comes inside the else statement.
                //So we need to check for permission again.
                healthConnectUtil.scope.launch {
                    healthConnectUtil.checkPermissionsAndRun(afterRequestingPermission = true)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cordova_fitness)

        val magicLink = intent.getStringExtra("ssoLink")!!

        Timber.d("mytag: CordovaFitnessActivity magicLink: $magicLink")



        webView = findViewById<WebView>(R.id.webView)

        val settings: WebSettings = webView.getSettings()
        settings.setJavaScriptEnabled(true)
        settings.setJavaScriptCanOpenWindowsAutomatically(true)
        settings.setBuiltInZoomControls(false)
        settings.setGeolocationEnabled(true)

        settings.setMediaPlaybackRequiresUserGesture(false)
        settings.setDomStorageEnabled(true)

        // Multiple Windows set to true to mitigate Chromium security bug.
        // See: https://bugs.chromium.org/p/chromium/issues/detail?id=1083819
        settings.setSupportMultipleWindows(true)
        webView.requestFocus()
        webView.requestFocusFromTouch()


        settings.setLoadWithOverviewMode(true)
        settings.setUseWideViewPort(true)


        // Enable Thirdparty Cookies
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.setWebChromeClient(object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                Timber.d("mytag :InAppChromeClient onCreateWindow")

                val inAppWebView: WebView = view
                val webViewClient: WebViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView, request: WebResourceRequest
                    ): Boolean {
                        inAppWebView.loadUrl(request.getUrl().toString())
                        return true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        inAppWebView.loadUrl(url)
                        return true
                    }
                }

                val newWebView: WebView = WebView(view.getContext())
                newWebView.setWebViewClient(webViewClient)

                val transport: WebView.WebViewTransport = resultMsg.obj as WebView.WebViewTransport
                transport.setWebView(newWebView)
                resultMsg.sendToTarget()

                return true
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                Timber.d("mytag :File Chooser 5.0+")
                // If callback exists, finish it.
                if (mUploadCallback != null) {
                    mUploadCallback!!.onReceiveValue(null)
                }
                mUploadCallback = filePathCallback

                // Create File Chooser Intent
                val content: Intent = Intent(Intent.ACTION_GET_CONTENT)
                content.addCategory(Intent.CATEGORY_OPENABLE)
                content.setType("*/*")

                // Run cordova startActivityForResult
                startActivityForResult(
                    Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE
                )
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String, callback: GeolocationPermissions.Callback
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback.invoke(origin, true, false)
            }
        })

        webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                webView: WebView, request: WebResourceRequest
            ): Boolean {
                return shouldOverrideUrlLoading(request.getUrl().toString(), request.getMethod())
            }


            fun shouldOverrideUrlLoading(url: String, method: String?): Boolean {
                var override = false

                if (url.startsWith(WebView.SCHEME_TEL)) {
                    try {
                        val intent: Intent = Intent(Intent.ACTION_DIAL)
                        intent.setData(Uri.parse(url))
                        startActivity(intent)
                        override = true
                    } catch (e: ActivityNotFoundException) {
                        Timber.d("mytag : Error dialing " + url + ": " + e.toString())
                    }
                } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith(
                        "market:"
                    ) || url.startsWith("intent:")
                ) {
                    try {
                        val intent: Intent = Intent(Intent.ACTION_VIEW)
                        intent.setData(Uri.parse(url))
                        startActivity(intent)
                        override = true
                    } catch (e: ActivityNotFoundException) {
                        Timber.d("mytag : Error with " + url + ": " + e.toString())
                    }
                } else if (url.startsWith("sms:")) {
                    try {
                        val intent: Intent = Intent(Intent.ACTION_VIEW)

                        // Get address
                        var address: String? = null
                        val parmIndex = url.indexOf('?')
                        if (parmIndex == -1) {
                            address = url.substring(4)
                        } else {
                            address = url.substring(4, parmIndex)

                            // If body, then set sms body
                            val uri = Uri.parse(url)
                            val query = uri.query
                            if (query != null) {
                                if (query.startsWith("body=")) {
                                    intent.putExtra("sms_body", query.substring(5))
                                }
                            }
                        }
                        intent.setData(Uri.parse("sms:$address"))
                        intent.putExtra("address", address)
                        intent.setType("vnd.android-dir/mms-sms")
                        startActivity(intent)
                        override = true
                    } catch (e: ActivityNotFoundException) {
                        Timber.d("mytag : Error sending sms " + url + ":" + e.toString())
                    }
                }
                return override
            }

            fun shouldInterceptRequest(
                url: String?, response: WebResourceResponse, method: String?
            ): WebResourceResponse {
                return response
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                var newloc = ""
                if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                    newloc = url
                } else {
                    // Assume that everything is HTTP at this point, because if we don't specify,
                    // it really should be. Complain loudly about this!!!
                    Timber.d("mytag : Possible Uncaught/Unknown URI")
                    newloc = "http://$url"
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

                // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device
                // storage
                CookieManager.getInstance().flush()

                // https://issues.apache.org/jira/browse/CB-11248
                view.clearFocus()
                view.requestFocus()
            }

            override fun onReceivedError(
                view: WebView, errorCode: Int, description: String, failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
            }
        })


        // 1. set background color
        webView.setBackgroundColor(Color.parseColor("#FFFFFF"))

        // // 2. add downloadlistener
        webView.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String,
                userAgent: String,
                contentDisposition: String,
                mimetype: String,
                contentLength: Long
            ) {
                Timber.d("mytag :DownloadListener called")

                try {
                    val uri = Uri.parse(url)
                    webView.getContext().startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })

        webAppInterface = WebAppInterface(this)

        visitStepSyncHelper = VisitStepSyncHelper(this)
        healthConnectUtil = HealthConnectUtil(this, this)


        webView.addJavascriptInterface(webAppInterface, "Android")
        webView.loadUrl(magicLink)

        healthConnectUtil.initialize()

        webView.loadUrl(magicLink)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }


    /**
     * Receive File Data from File Chooser
     *
     * @param requestCode the requested code from chromeclient
     * @param resultCode  the result code returned from android system
     * @param intent      the data from android file chooser
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d(
            "mytag: CordovaFitnessPlugin onActivityResult called. requestCode: " + requestCode + " resultCode: " + resultCode
        )


        if (requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent)
            return
        }
        if (mUploadCallback != null) {
            mUploadCallback!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode, intent
                )
            )
        }
        mUploadCallback = null
    }

    /**
     * This get called from the webview when user taps on [Connect To Google Fit]
     */
    override fun askForPermissions() {
        if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.CONNECTED) {
            healthConnectUtil.getVisitDashboardGraph()
        } else {
            healthConnectUtil.requestPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            LOCATION_PERMISSION_REQUEST_CODE -> {}
        }
    }


    override fun requestActivityData(type: String, frequency: String, timestamp: Long) {
        Timber.d("mytag: requestActivityData() called.")

        //Health Connect Implementation
        healthConnectUtil.getActivityData(type, frequency, timestamp)
    }

    override fun syncDataWithServer(
        baseUrl: String, authToken: String, googleFitLastSync: Long, gfHourlyLastSync: Long
    ) {
        Timber.d("mytag: baseUrl: $baseUrl")
        if (!syncDataWithServer) {
            Timber.d("mytag: syncDataWithServer() called")

            visitStepSyncHelper.sendDataToVisitServer(
                healthConnectUtil, googleFitLastSync, gfHourlyLastSync, "$baseUrl/", authToken!!
            )

            syncDataWithServer = true
        }
    }

    override fun getHealthConnectStatus() {
        if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.NOT_SUPPORTED) {
            Handler(Looper.getMainLooper()).post {
                val finalString = "window.healthConnectNotSupported()"
                webView.evaluateJavascript(
                    finalString, null
                )
            }
        } else if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.NOT_INSTALLED) {
            Handler(Looper.getMainLooper()).post {
                val finalString = "window.healthConnectNotInstall()"
                webView.evaluateJavascript(
                    finalString, null
                )
            }
        } else if (healthConnectUtil.healthConnectConnectionState == HealthConnectConnectionState.INSTALLED) {
            Handler(Looper.getMainLooper()).post {
                val finalString = "window.healthConnectAvailable()"
                webView.evaluateJavascript(
                    finalString, null
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun askForLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this, LOCATION_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(LOCATION_PERMISSION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    fun closeVisitPWA() {
        finish()
    }

    override fun updateHealthConnectConnectionStatus(
        status: HealthConnectConnectionState, text: String
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

    override fun loadVisitWebViewGraphData(webUrl: String) {
        Timber.d("loadVisitWebViewGraphData: webUrl: $webUrl")

        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(
                webUrl, null
            )
        }
    }

    override fun userDeniedHealthConnectPermission() {
        Timber.d("mytag : userDeniedHealthConnectPermission")
    }

    override fun userAcceptedHealthConnectPermission() {
        Timber.d("mytag : userAcceptedHealthConnectPermission")
    }

    override fun requestPermission() {
        requestPermissions.launch(healthConnectUtil.PERMISSIONS)
    }
}