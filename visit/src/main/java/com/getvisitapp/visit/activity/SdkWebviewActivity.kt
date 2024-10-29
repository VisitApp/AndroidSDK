package com.getvisitapp.visit.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.getvisitapp.visit.R
import com.getvisitapp.visit.connectivity.ConnectivityObserver
import com.getvisitapp.visit.connectivity.NetworkConnectivityObserver
import com.getvisitapp.visit.data.WebAppInterface
import com.getvisitapp.visit.util.Constants.IS_DEBUG
import com.getvisitapp.visit.util.Constants.WEB_URL
import com.getvisitapp.visit.util.LocationTrackerUtil
import com.getvisitapp.visit.util.PdfDownloader
import com.getvisitapp.visit.util.makeStatusBarTransparent
import com.getvisitapp.visit.view.GoogleFitStatusListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber


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


    val LOCATION_PERMISSION_REQUEST_CODE = 787
    val REQUEST_CODE_FILE_PICKER = 51426


    var isDebug: Boolean = false
    lateinit var magicLink: String


    lateinit var pdfDownloader: PdfDownloader


    lateinit var locationTrackerUtil: LocationTrackerUtil
    private val AUTHORITY_SUFFIX = ".googlefitsdk.fileprovider"

    lateinit var connectivityObserver: ConnectivityObserver
    var mFileUploadCallbackSecond: ValueCallback<Array<Uri>>? = null


    var webChromeClient: WebChromeClient = MyChrome()
    var webViewClient: WebViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Timber.tag(TAG).d("onPageStarted: $url")
            progressBar.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Timber.tag(TAG).d("onPageFinished: $url")
            progressBar.visibility = View.GONE

        }


        override fun onReceivedError(
            view: WebView?, request: WebResourceRequest?, error: WebResourceError?
        ) {

//            Timber.tag(TAG)
//                .d( "errorCode: $errorCode description: $description failingUrl: $failingUrl")
            progressBar.visibility = View.GONE
            Timber.tag(TAG).d("onReceivedError")
        }


        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            Timber.tag(TAG).d("shouldOverrideUrlLoading")

            url?.let {
                webview.loadUrl(url)
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

        var userEventCallback: ((eventName: String) -> Unit)? = null
        var errorEventCallback: ((errorMessage: String, description: String?) -> Unit)? = null
    }

    lateinit var progressBar: ProgressBar
    lateinit var webview: WebView
    lateinit var noNetworkConnectionLayout: LinearLayout
    lateinit var parentLayout: ConstraintLayout


    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeStatusBarTransparent()
        setContentView(R.layout.visit_activity_sdk)
        progressBar = findViewById(R.id.progressBar)
        webview = findViewById(R.id.webview)
        noNetworkConnectionLayout = findViewById(R.id.noNetworkConnectionLayout)
        parentLayout = findViewById(R.id.parentLayout)

        progressBar.setVisibility(View.VISIBLE)
        magicLink = intent.extras!!.getString(WEB_URL)!!
        isDebug = intent.extras!!.getBoolean(IS_DEBUG);

        if (isDebug) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webview.settings.javaScriptEnabled = true
        webview.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true)
        webview.settings.setGeolocationEnabled(true)
        webview.settings.domStorageEnabled = true;
        webview.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        webview.webChromeClient = webChromeClient
        webview.webViewClient = webViewClient

        webview.setDownloadListener(object : DownloadListener {
            override fun onDownloadStart(
                url: String?,
                userAgent: String?,
                contentDisposition: String?,
                mimetype: String?,
                contentLength: Long
            ) {

                Timber.tag(TAG).d("onDownloadRequested() url:$url, mimeType:$mimetype");

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
                            Timber.tag(TAG).d(
                                "onDownloadRequested() download failed, opening it in chrome"
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

        webview.loadUrl(magicLink)


        webAppInterface = WebAppInterface(this)
        webview.addJavascriptInterface(webAppInterface, "Android")

        pdfDownloader = PdfDownloader()
        locationTrackerUtil = LocationTrackerUtil(this)

        connectivityObserver = NetworkConnectivityObserver(this)

        connectivityObserver.observe().onEach { networkStatus ->
            when (networkStatus) {
                ConnectivityObserver.Status.Available -> {
                    noNetworkConnectionLayout.visibility = View.GONE
                }

                ConnectivityObserver.Status.Unavailable -> {
                    noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Losing -> {
                    noNetworkConnectionLayout.visibility = View.VISIBLE
                }

                ConnectivityObserver.Status.Lost -> {
                    noNetworkConnectionLayout.visibility = View.VISIBLE
                }
            }
            Timber.tag(TAG).d("network status: $networkStatus")
        }.launchIn(lifecycleScope)


        ViewCompat.setOnApplyWindowInsetsListener(parentLayout) { view, windowInsets ->


            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets =
                windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())


//            Timber.tag(
//                TAG
//            ).d(
//                "imeInsets(bottom: ${imeInsets.bottom}), navigationBarInsets: (bottom: ${navigationBarInsets.bottom}), imeVisible: ${imeVisible} "
//            )

            webview.updateLayoutParams<ConstraintLayout.LayoutParams> {
                this.bottomMargin = navigationBarInsets.bottom
            }

            if (imeVisible) {
                webview.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    this.bottomMargin = imeInsets.bottom
                }
            } else {
                webview.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    this.bottomMargin = 0
                }
            }

//
//            // Return CONSUMED if you don't want want the window insets to keep passing
//            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }


    }

    override fun onResume() {
        super.onResume()
        webview.onResume();
    }

    override fun onPause() {
        webview.onPause();
        super.onPause()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.tag(TAG)
            .d("onActivityResult called. requestCode: $requestCode resultCode: $resultCode")

        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == 1000 && resultCode == RESULT_OK) {
            Timber.tag(TAG).d("resultCode: $requestCode")

            webview.webChromeClient = webChromeClient
            webview.webViewClient = webViewClient


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

    override fun askForLocationPermission() {

        runOnUiThread {
            if (locationTrackerUtil.isLocationPermissionAllowed()) {
                if (locationTrackerUtil.isGPSEnabled()) {
                    runOnUiThread {
                        webview.evaluateJavascript(
                            "window.checkTheGpsPermission(true)", null
                        )
                        Timber.tag(TAG).d("window.checkTheGpsPermission(true) called")
                    }
                } else {
                    locationTrackerUtil.showGPS_NotEnabledDialog()
                }
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }


    }

    override fun visitCallback(jsonObject: String?) {
        Timber.tag(TAG).d("visitCallback jsonObject: $jsonObject")

        jsonObject?.let {

            val eventData = getAllKeysAndValues(Gson().fromJson(jsonObject, JsonObject::class.java))
            Timber.tag(TAG).d(
                "setUserEventCallback eventJsonObject: $jsonObject, eventData: $eventData"
            )

            val eventName = eventData["eventName"].toString()

            userEventCallback?.invoke(eventName)
        }
    }

    override fun errorCallback(jsonObject: String?) {
        Timber.tag(TAG).d("errorCallback jsonObject: $jsonObject")

        jsonObject?.let {

            val eventData = getAllKeysAndValues(Gson().fromJson(jsonObject, JsonObject::class.java))


            Timber.tag(TAG).d(
                "setUserEventCallback eventJsonObject: $jsonObject, eventData: $eventData"
            )

            val errorTitle: String = eventData["errorTitle"].toString()
            val errorDesc: String? = eventData["errorDesc"]?.toString()

            errorEventCallback?.invoke(errorTitle, errorDesc)
        }

    }

    private fun getAllKeysAndValues(jsonObject: JsonObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()

        jsonObject.keySet().forEach { key ->
            map[key] = jsonObject.get(key)
        }

        return map
    }

    override fun onRestart() {
        super.onRestart()
        if (locationTrackerUtil.isLocationPermissionAllowed() && locationTrackerUtil.isGPSEnabled()) {
            runOnUiThread {
                webview.evaluateJavascript(
                    "window.checkTheGpsPermission(true)", null
                )
                Timber.tag(TAG).d("window.checkTheGpsPermission(true) called")
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
                                webview.evaluateJavascript(
                                    "window.checkTheGpsPermission(true)", null
                                )
                                Timber.tag(TAG).d("window.checkTheGpsPermission(true) called")
                            }
                        }
                    } else {
                        locationTrackerUtil.showLocationPermissionDeniedAlertDialog()
                    }
                }
            }
        }
    }

    override fun closeView() {
        finish()
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {

                    Timber.tag(TAG)
                        .d("webview.canGoBack(): ${webview.canGoBack()}, url: ${webview.url}")

                    if (webview.canGoBack()) {
                        webview.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("onDestroy called")
        userEventCallback = null
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
                getApplicationContext().resources, 2130837573
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
            origin: String?, callback: GeolocationPermissions.Callback?
        ) {
            Timber.tag(TAG).d("onGeolocationPermissionsShowPrompt called")
            super.onGeolocationPermissionsShowPrompt(origin, callback);
            callback?.invoke(origin, true, false);
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Timber.tag(TAG).d("onGeolocationPermissionsHidePrompt called")
            super.onGeolocationPermissionsHidePrompt()

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webview.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webview.restoreState(savedInstanceState)
    }
}




