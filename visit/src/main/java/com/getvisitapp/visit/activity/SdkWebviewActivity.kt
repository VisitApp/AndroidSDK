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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
import org.json.JSONObject
import timber.log.Timber
import java.io.File


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


    var isDebug: Boolean = false
    lateinit var magicLink: String


    lateinit var pdfDownloader: PdfDownloader


    lateinit var locationTrackerUtil: LocationTrackerUtil
    private val AUTHORITY_SUFFIX = ".googlefitsdk.fileprovider"

    lateinit var connectivityObserver: ConnectivityObserver
    var mFileUploadCallbackSecond: ValueCallback<Array<Uri>>? = null
    private var pendingCameraCaptureUri: Uri? = null
    private var pendingCameraCaptureFile: File? = null
    private var isCameraCaptureInProgress = false

    private data class PendingGeolocationPermissionRequest(
        val origin: String?,
        val callback: GeolocationPermissions.Callback,
    )

    private val pendingGeolocationPermissionRequests =
        mutableListOf<PendingGeolocationPermissionRequest>()
    private var isResolvingLocationAccessRequest = false

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val preciseLocationGranted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    locationTrackerUtil.isPreciseLocationPermissionAllowed()

        Timber.tag(TAG).d(
            "location permission ActivityResult received: permissions=$permissions, preciseLocationGranted=$preciseLocationGranted"
        )

        if (preciseLocationGranted) {
            requestGpsSettings(allowResolution = true)
        } else {
            finishLocationAccessRequest(granted = false)
        }
    }

    private val gpsSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        Timber.tag(TAG).d(
            "GPS settings ActivityResult received: resultCode=${result.resultCode}"
        )
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.tag(TAG).d(
                "GPS settings ActivityResult OK; re-checking GPS settings with allowResolution=false and one delayed retry"
            )
            requestGpsSettings(allowResolution = false, remainingDelayedRetries = 1)
        } else {
            Timber.tag(TAG).d("GPS settings ActivityResult cancelled; completing granted=false")
            finishLocationAccessRequest(granted = false)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.tag(TAG).d("camera permission ActivityResult received: isGranted=$isGranted")
        if (isGranted) {
            launchCameraCapture()
        } else {
            finishCameraCaptureRequest(
                granted = false,
                uri = null,
                errorCode = CAMERA_ERROR_PERMISSION_DENIED
            )
        }
    }

    private val cameraCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isCaptured ->
        val capturedUri = pendingCameraCaptureUri
        Timber.tag(TAG).d(
            "camera capture ActivityResult received: isCaptured=$isCaptured, uri=$capturedUri"
        )
        if (isCaptured && capturedUri != null) {
            finishCameraCaptureRequest(
                granted = true,
                uri = capturedUri,
                errorCode = null,
                deletePendingFile = false
            )
        } else {
            finishCameraCaptureRequest(
                granted = false,
                uri = null,
                errorCode = if (isCaptured) CAMERA_ERROR_CAPTURE_FAILED else CAMERA_ERROR_CAPTURE_CANCELLED
            )
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dataUris = if (result.resultCode == Activity.RESULT_OK) {
            getSelectedFileUris(result.data)
        } else {
            null
        }

        Timber.tag(TAG).d(
            "file picker ActivityResult received: resultCode=${result.resultCode}, hasData=${result.data != null}, selectedUriCount=${dataUris?.size ?: 0}"
        )

        mFileUploadCallbackSecond?.onReceiveValue(dataUris)
        mFileUploadCallbackSecond = null
    }


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
        private const val CAMERA_PERMISSION_TYPE = "CAMERA"
        private const val CAMERA_CAPTURE_DIRECTORY = "files/cameraCaptures"
        private const val CAMERA_CAPTURE_FILE_PREFIX = "camera_capture_"
        private const val CAMERA_CAPTURE_FILE_SUFFIX = ".jpg"
        private const val CAMERA_ERROR_UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE"
        private const val CAMERA_ERROR_PERMISSION_DENIED = "PERMISSION_DENIED"
        private const val CAMERA_ERROR_CAMERA_UNAVAILABLE = "CAMERA_UNAVAILABLE"
        private const val CAMERA_ERROR_CAPTURE_CANCELLED = "CAPTURE_CANCELLED"
        private const val CAMERA_ERROR_CAPTURE_FAILED = "CAPTURE_FAILED"
        private const val CAMERA_ERROR_REQUEST_IN_PROGRESS = "REQUEST_IN_PROGRESS"

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

        webAppInterface = WebAppInterface(this)
        webview.addJavascriptInterface(webAppInterface, "Android")

        pdfDownloader = PdfDownloader()
        locationTrackerUtil = LocationTrackerUtil(this)

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


        val initialParentPaddingLeft = parentLayout.paddingLeft
        val initialParentPaddingTop = parentLayout.paddingTop
        val initialParentPaddingRight = parentLayout.paddingRight
        val initialParentPaddingBottom = parentLayout.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(parentLayout) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val bottomInset = maxOf(systemBars.bottom, imeInsets.bottom)

            Timber.tag(TAG).d(
                "window insets applied: systemBars=$systemBars, ime=$imeInsets, imeVisible=$imeVisible, bottomInset=$bottomInset"
            )

            parentLayout.setPadding(
                initialParentPaddingLeft + systemBars.left,
                initialParentPaddingTop,
                initialParentPaddingRight + systemBars.right,
                initialParentPaddingBottom + bottomInset
            )

            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(parentLayout)


    }

    override fun onResume() {
        super.onResume()
        webview.onResume();
    }

    override fun onPause() {
        webview.onPause();
        super.onPause()
    }


    private fun startLocationAccessRequest(
        origin: String? = null,
        callback: GeolocationPermissions.Callback? = null,
    ) {
        runOnUiThread {
            callback?.let {
                pendingGeolocationPermissionRequests.add(
                    PendingGeolocationPermissionRequest(origin, it)
                )
            }

            if (isResolvingLocationAccessRequest) {
                return@runOnUiThread
            }

            isResolvingLocationAccessRequest = true

            if (locationTrackerUtil.isPreciseLocationPermissionAllowed()) {
                requestGpsSettings(allowResolution = true)
            } else {
                try {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).d("locationPermissionLauncher failed: ${e.message}")
                    finishLocationAccessRequest(granted = false)
                }
            }
        }
    }

    private fun requestGpsSettings(
        allowResolution: Boolean,
        remainingDelayedRetries: Int = 0,
    ) {
        runOnUiThread {
            Timber.tag(TAG).d(
                "requestGpsSettings called: allowResolution=$allowResolution, remainingDelayedRetries=$remainingDelayedRetries"
            )
            locationTrackerUtil.promptUserToTurnOnGPS(
                onSuccessListener = {
                    Timber.tag(TAG).d(
                        "GPS settings onSuccessListener called: allowResolution=$allowResolution, remainingDelayedRetries=$remainingDelayedRetries, completing granted=true"
                    )
                    finishLocationAccessRequest(granted = true)
                },
                onResolutionRequiredListener = { intentSenderRequest: IntentSenderRequest ->
                    Timber.tag(TAG).d(
                        "GPS settings onResolutionRequiredListener called: allowResolution=$allowResolution, remainingDelayedRetries=$remainingDelayedRetries"
                    )
                    if (allowResolution) {
                        try {
                            Timber.tag(TAG).d("launching GPS settings resolution prompt")
                            gpsSettingsLauncher.launch(intentSenderRequest)
                        } catch (e: Exception) {
                            Timber.tag(TAG).d("gpsSettingsLauncher failed: ${e.message}")
                            finishLocationAccessRequest(granted = false)
                        }
                    } else if (remainingDelayedRetries > 0) {
                        Timber.tag(TAG).d(
                            "GPS settings immediate re-check still requires resolution; scheduling delayed retry in 500ms"
                        )
                        lifecycleScope.launch {
                            delay(500)
                            Timber.tag(TAG).d(
                                "executing delayed GPS settings retry; remainingDelayedRetries=${remainingDelayedRetries - 1}"
                            )
                            requestGpsSettings(
                                allowResolution = false,
                                remainingDelayedRetries = remainingDelayedRetries - 1
                            )
                        }
                    } else {
                        Timber.tag(TAG).d(
                            "GPS settings retry exhausted and still requires resolution; completing granted=false"
                        )
                        finishLocationAccessRequest(granted = false)
                    }
                },
                onFailureListener = { exception ->
                    Timber.tag(TAG).d(
                        "GPS settings onFailureListener called: exception=${exception::class.java.simpleName}, message=${exception.message}"
                    )
                    finishLocationAccessRequest(granted = false)
                }
            )
        }
    }

    private fun finishLocationAccessRequest(granted: Boolean) {
        runOnUiThread {
            if (!isResolvingLocationAccessRequest && pendingGeolocationPermissionRequests.isEmpty()) {
                return@runOnUiThread
            }

            isResolvingLocationAccessRequest = false
            sendLegacyLocationPermissionCallback(granted)

            val pendingRequests = pendingGeolocationPermissionRequests.toList()
            pendingGeolocationPermissionRequests.clear()
            pendingRequests.forEach { request ->
                Timber.tag(TAG).d(
                    "sending WebView geolocation callback: origin=${request.origin}, granted=$granted"
                )
                request.callback.invoke(request.origin, granted, false)
            }
        }
    }

    private fun sendLegacyLocationPermissionCallback(granted: Boolean) {
        if (!::webview.isInitialized) {
            return
        }

        val script =
            "window.checkTheGpsPermission($granted)"
        Timber.tag(TAG).d("sending JS GPS callback: window.checkTheGpsPermission($granted)")
        webview.evaluateJavascript(script) { result ->
            Timber.tag(TAG).d(
                "sent JS GPS callback: window.checkTheGpsPermission($granted), result=$result"
            )
        }
    }

    private fun startCameraPermissionRequest() {
        if (isCameraCaptureInProgress) {
            Timber.tag(TAG).d("camera request ignored because another request is in progress")
            sendCameraPermissionCallback(
                granted = false,
                uri = null,
                errorCode = CAMERA_ERROR_REQUEST_IN_PROGRESS
            )
            return
        }

        isCameraCaptureInProgress = true

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            finishCameraCaptureRequest(
                granted = false,
                uri = null,
                errorCode = CAMERA_ERROR_CAMERA_UNAVAILABLE
            )
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchCameraCapture()
        } else {
            try {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                Timber.tag(TAG).d("cameraPermissionLauncher failed: ${e.message}")
                finishCameraCaptureRequest(
                    granted = false,
                    uri = null,
                    errorCode = CAMERA_ERROR_PERMISSION_DENIED
                )
            }
        }
    }

    private fun launchCameraCapture() {
        try {
            val outputUri = createCameraCaptureUri()
            Timber.tag(TAG).d("launching camera capture with uri=$outputUri")
            cameraCaptureLauncher.launch(outputUri)
        } catch (e: Exception) {
            Timber.tag(TAG).d("camera capture launch failed: ${e.message}")
            finishCameraCaptureRequest(
                granted = false,
                uri = null,
                errorCode = CAMERA_ERROR_CAPTURE_FAILED
            )
        }
    }

    private fun createCameraCaptureUri(): Uri {
        val captureDir = File(filesDir, CAMERA_CAPTURE_DIRECTORY)
        if (!captureDir.exists() && !captureDir.mkdirs()) {
            error("Unable to create camera capture directory")
        }

        val captureFile = File.createTempFile(
            CAMERA_CAPTURE_FILE_PREFIX,
            CAMERA_CAPTURE_FILE_SUFFIX,
            captureDir
        )
        pendingCameraCaptureFile = captureFile

        val captureUri = FileProvider.getUriForFile(
            this,
            applicationContext.packageName + AUTHORITY_SUFFIX,
            captureFile
        )

        pendingCameraCaptureUri = captureUri
        return captureUri
    }

    private fun finishCameraCaptureRequest(
        granted: Boolean,
        uri: Uri?,
        errorCode: String?,
        deletePendingFile: Boolean = true
    ) {
        runOnUiThread {
            if (!granted && deletePendingFile) {
                deletePendingCameraCaptureFile()
            }

            sendCameraPermissionCallback(
                granted = granted,
                uri = uri,
                errorCode = errorCode
            )
            clearPendingCameraCapture(deletePendingFile = false)
        }
    }

    private fun sendCameraPermissionCallback(
        granted: Boolean,
        uri: Uri?,
        errorCode: String?
    ) {
        if (!::webview.isInitialized) {
            return
        }

        val uriArgument = uri?.toString()?.let { JSONObject.quote(it) } ?: "null"
        val errorArgument = errorCode?.let { JSONObject.quote(it) } ?: "null"
        val script = "window.checkTheCameraPermission($granted, $uriArgument, $errorArgument)"
        Timber.tag(TAG).d("sending JS camera callback: $script")
        webview.evaluateJavascript(script) { result ->
            Timber.tag(TAG).d("sent JS camera callback, result=$result")
        }
    }

    private fun clearPendingCameraCapture(deletePendingFile: Boolean) {
        if (deletePendingFile) {
            deletePendingCameraCaptureFile()
        }
        pendingCameraCaptureUri = null
        pendingCameraCaptureFile = null
        isCameraCaptureInProgress = false
    }

    private fun deletePendingCameraCaptureFile() {
        try {
            val file = pendingCameraCaptureFile
            if (file?.exists() == true) {
                file.delete()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d("delete pending camera capture failed: ${e.message}")
        }
    }

    private fun getSelectedFileUris(intent: Intent?): Array<Uri>? {
        if (intent == null) {
            return null
        }

        intent.data?.let { uri ->
            return arrayOf(uri)
        }

        val clipData = intent.clipData ?: return null
        if (clipData.itemCount == 0) {
            return null
        }

        return Array(clipData.itemCount) { index ->
            clipData.getItemAt(index).uri
        }
    }

    override fun visitCallback(jsonObject: String?) {
        Timber.tag(TAG).d("web event received: visitCallback")

        jsonObject?.let {

            val eventData = getAllKeysAndValues(Gson().fromJson(jsonObject, JsonObject::class.java))
            Timber.tag(TAG).d(
                "setUserEventCallback eventJsonObject: $jsonObject, eventData: $eventData"
            )

            val eventName = eventData["eventName"].toString()

            Timber.tag(TAG).d("web event received: visitCallback eventName=$eventName")
            Timber.tag(TAG).d("sending SDK host event callback: eventName=$eventName")
            userEventCallback?.invoke(eventName)
        }
    }

    override fun errorCallback(jsonObject: String?) {
        Timber.tag(TAG).d("web event received: errorCallback")

        jsonObject?.let {

            val eventData = getAllKeysAndValues(Gson().fromJson(jsonObject, JsonObject::class.java))


            Timber.tag(TAG).d(
                "setUserEventCallback eventJsonObject: $jsonObject, eventData: $eventData"
            )

            val errorTitle: String = eventData["errorTitle"].toString()
            val errorDesc: String? = eventData["errorDesc"]?.toString()

            Timber.tag(TAG).d(
                "web event received: errorCallback errorTitle=$errorTitle, errorDesc=$errorDesc"
            )
            Timber.tag(TAG).d(
                "sending SDK host error callback: errorTitle=$errorTitle, errorDesc=$errorDesc"
            )
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

    override fun askForLocationPermission() {
        Timber.tag(TAG).d("web event received: getLocationPermissions")
        runOnUiThread {
            startLocationAccessRequest()
        }
    }

    override fun requestPermission(type: String?) {
        Timber.tag(TAG).d("web event received: requestPermission type=$type")
        runOnUiThread {
            if (!type.equals(CAMERA_PERMISSION_TYPE, ignoreCase = true)) {
                sendCameraPermissionCallback(
                    granted = false,
                    uri = null,
                    errorCode = CAMERA_ERROR_UNSUPPORTED_TYPE
                )
                return@runOnUiThread
            }

            startCameraPermissionRequest()
        }
    }

    override fun closeView() {
        Timber.tag(TAG).d("web event received: closeView")
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
        finishLocationAccessRequest(granted = false)
        clearPendingCameraCapture(deletePendingFile = true)
        userEventCallback = null
        super.onDestroy()
    }


    override fun openLink(url: String?) {
        Timber.tag(TAG).d("web event received: openLink url=$url")
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

            Timber.d("filePath: $filePathCallback, fileChooserParams: $fileChooserParams")

            if (mFileUploadCallbackSecond != null) {
                mFileUploadCallbackSecond!!.onReceiveValue(null)
            }
            if (filePathCallback == null) {
                return false
            }
            mFileUploadCallbackSecond = filePathCallback

            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)

            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

            i.type = "*/*"

            filePickerLauncher.launch(Intent.createChooser(i, "Choose a file"))

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
            Timber.tag(TAG).d(
                "web geolocation request received: origin=$origin, hasCallback=${callback != null}"
            )
            startLocationAccessRequest(origin, callback)
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Timber.tag(TAG).d("web geolocation request hidden")
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
