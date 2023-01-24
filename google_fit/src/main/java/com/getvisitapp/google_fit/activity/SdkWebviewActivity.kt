package com.getvisitapp.google_fit.activity

import android.Manifest
import android.app.Activity
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.getvisitapp.google_fit.R
import com.getvisitapp.google_fit.data.GoogleFitUtil
import com.getvisitapp.google_fit.data.SharedPrefUtil
import com.getvisitapp.google_fit.databinding.SdkWebView
import com.getvisitapp.google_fit.util.Constants.DEFAULT_CLIENT_ID
import com.getvisitapp.google_fit.util.Constants.IS_DEBUG
import com.getvisitapp.google_fit.util.Constants.WEB_URL
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.getvisitapp.google_fit.util.PdfDownloader
import com.getvisitapp.google_fit.view.GoogleFitStatusListener
import com.getvisitapp.google_fit.view.VideoCallListener
import im.delight.android.webview.AdvancedWebView
import java.util.*

class SdkWebviewActivity : AppCompatActivity(), AdvancedWebView.Listener,
    VideoCallListener, GoogleFitStatusListener {

    var TAG = "mytag"

    lateinit var binding: SdkWebView


    val ACTIVITY_RECOGNITION_REQUEST_CODE = 490
    val LOCATION_PERMISSION_REQUEST_CODE = 787
    lateinit var googleFitUtil: GoogleFitUtil

    var isDebug: Boolean = false
    lateinit var magicLink: String
    lateinit var default_web_client_id: String


    var dailyDataSynced = false
    var syncDataWithServer = false

    private lateinit var googleFitStepChecker: GoogleFitAccessChecker

    lateinit var sharedPrefUtil: SharedPrefUtil
    lateinit var pdfDownloader: PdfDownloader


    var visitApiBaseUrl: String? = null
    var authtoken: String? = null
    var googleFitLastSync: Long = 0L
    var gfHourlyLastSync = 0L
    var memberId: String? = null

    private val AUTHORITY_SUFFIX = ".googlefitsdk.fileprovider"


    companion object {
        fun getIntent(
            context: Context,
            isDebug: Boolean,
            magicLink: String,
            default_web_client_id: String
        ): Intent {
            val intent = Intent(context, SdkWebviewActivity::class.java);
            intent.putExtra(IS_DEBUG, isDebug)
            intent.putExtra(WEB_URL, magicLink)
            intent.putExtra(DEFAULT_CLIENT_ID, default_web_client_id)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sdk)
        binding.infoView.setVisibility(View.GONE)
        magicLink = intent.extras!!.getString(WEB_URL)!!
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
            GoogleFitUtil(this, this, default_web_client_id)
        binding.webview.addJavascriptInterface(googleFitUtil.webAppInterface, "Android")
        googleFitUtil.init()

        googleFitStepChecker = GoogleFitAccessChecker(this)
        sharedPrefUtil = SharedPrefUtil(this)
        pdfDownloader = PdfDownloader()

    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume();
    }

    override fun onPause() {
        binding.webview.onPause();
        super.onPause()
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

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onFitnessPermissionGranted() {


        Log.d(TAG, "onFitnessPermissionGranted() called")

        Log.d(TAG, "window.googleFitnessConnectedSuccessfully() called")

        runOnUiThread {
            binding.webview.evaluateJavascript(
                "window.googleFitnessConnectedSuccessfully(true)",
                null
            )
        }

        //manually calling sync steps here because we are not getting sync step event after the google fit is connected
        if (visitApiBaseUrl != null &&
            authtoken != null &&
            googleFitLastSync != 0L &&
            gfHourlyLastSync != 0L &&
            memberId != null
        ) {
            runOnUiThread {
                googleFitUtil.sendDataToServer(
                    visitApiBaseUrl + "/",
                    authtoken,
                    googleFitLastSync,
                    gfHourlyLastSync
                )
                syncDataWithServer = true
            }
        }


//        runOnUiThread(Runnable { googleFitUtil.fetchDataFromFit() })
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
        visitApiBaseUrl: String?,
        authtoken: String?,
        googleFitLastSync: Long,
        gfHourlyLastSync: Long,
        memberId: String
    ) {

        this.visitApiBaseUrl = visitApiBaseUrl
        this.authtoken = authtoken
        this.googleFitLastSync = googleFitLastSync
        this.gfHourlyLastSync = gfHourlyLastSync
        this.memberId = memberId

        //For the first time, when the logs in the PWA, this will comes zero, so in that case just make it today's date
        if (this.googleFitLastSync == 0L) {
            this.googleFitLastSync = getTodayDateTimeStamp()
        }
        if (this.gfHourlyLastSync == 0L) {
            this.gfHourlyLastSync = getTodayDateTimeStamp()
        }


        Log.d("mytag", "apiBaseUrl: $visitApiBaseUrl $memberId")
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called")

            visitApiBaseUrl?.let {
                sharedPrefUtil.setVisitBaseUrl(visitApiBaseUrl + "/")
            }
            authtoken?.let {
                sharedPrefUtil.setVisitAuthToken(authtoken)
            }

            sharedPrefUtil.setGoogleFitDailyLastSyncTimeStamp(googleFitLastSync)
            sharedPrefUtil.setGoogleFitHourlyLastSyncTimeStamp(gfHourlyLastSync)
            sharedPrefUtil.setTataAIGLastSyncTimeStamp(gfHourlyLastSync)// adding this here because there might be a case where the user just connected to google fit and
            // closed TATA AIG app immediately, in that case take this timestamp and start syncing from there end.

            memberId?.let {
                sharedPrefUtil.setTATA_AIG_MemberId(memberId)
            }

            runOnUiThread(Runnable {
                googleFitUtil.sendDataToServer(
                    visitApiBaseUrl + "/",
                    authtoken,
                    googleFitLastSync,
                    gfHourlyLastSync,
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
                if (grantResults.isNotEmpty()) {
                    val fitnessPermissionGranted =
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    if (fitnessPermissionGranted) {
                        Log.d(TAG, "ACTIVITY_RECOGNITION_REQUEST_CODE permission granted")
                        googleFitUtil.askForGoogleFitPermission()
                    }
                }

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

    override fun hraCompleted() {}

    override fun googleFitConnectedAndSavedInPWA() {
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun inHraEndPage() {
        runOnUiThread {
            //check for google fit has access and call this event

            if (googleFitStepChecker.checkGoogleFitAccess()) {
                binding.webview.evaluateJavascript(
                    "window.showConnectToGoogleFit(false)",
                    null
                )
                Log.d("mytag", "showConnectToGoogleFit(false) called")
            } else {
                binding.webview.evaluateJavascript(
                    "window.showConnectToGoogleFit(true)",
                    null
                )
                Log.d("mytag", "showConnectToGoogleFit(true) called")
            }
        }
    }

    override fun hraQuestionAnswered(current: Int, total: Int) {
        runOnUiThread {
        }
    }


    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun inFitSelectScreen() {
        runOnUiThread {
            //check for google fit has access and call this event

            if (googleFitStepChecker.checkGoogleFitAccess()) {
                binding.webview.evaluateJavascript(
                    "window.googleFitStatus(true)",
                    null
                )
                Log.d("mytag", "googleFitStatus(true) called")
            } else {
                binding.webview.evaluateJavascript(
                    "window.googleFitStatus(false)",
                    null
                )
                Log.d("mytag", "googleFitStatus(false) called")
            }
        }
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
                        } else if (binding.webview.url!!.contains("online/preview")) {
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


    override fun onStart() {
        super.onStart()
    }


    override fun onStop() {
        super.onStop()
    }

    fun closePWA() {
        finish()
        overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        binding.webview.onDestroy();
        super.onDestroy()
    }

    fun getTodayDateTimeStamp(): Long {
        val startCalendar: Calendar = Calendar.getInstance()
        startCalendar.setTimeInMillis(System.currentTimeMillis())

        //doing to remove the hours passed in the today's date.

        //doing to remove the hours passed in the today's date.
        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startCalendar.set(Calendar.MINUTE, 0)
        startCalendar.set(Calendar.SECOND, 0)

        return startCalendar.getTimeInMillis()
    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {
        Log.d("mytag", "onDownloadRequested() url:$url, mimeType:$mimeType");

        url?.let {
            pdfDownloader.downloadPdfFile(
                fileDir = filesDir,
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
                }, onDownloadFailed = {
                    Log.d(TAG, "onDownloadRequested() download failed, opening it in chrome")
                    try {
                        val uri = Uri.parse(url)
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
        }

    }

    override fun downloadHraLink(url: String) {
        Log.d("mytag", "downloadHraLink() link:$url")

        pdfDownloader.downloadPdfFile(
            fileDir = filesDir,
            pdfUrl = url,
            onDownloadComplete = {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                            applicationContext, applicationContext.packageName + AUTHORITY_SUFFIX,
                            it
                        )
                    )
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    type = "application/pdf"
                }
                val sendIntent = Intent.createChooser(shareIntent, null)
                startActivity(sendIntent)
            }, onDownloadFailed = {
                Log.d(TAG, "downloadHraLink() download failed, opening it in chrome")
                try {
                    val uri = Uri.parse(url)
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
    }

    override fun openDependentLink(link: String?) {
        Log.d("mytag", "openDependentLink link:$link")
        val customIntent = CustomTabsIntent.Builder()
        customIntent.setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        customIntent.setShowTitle(false)
        customIntent.setToolbarColor(
            ContextCompat.getColor(
                this,
                R.color.tata_aig_brand_color
            )
        )
        try {
            val uri = Uri.parse(link)
            openCustomTab(this, customIntent.build(), uri = uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeView(tataUser: Boolean) {
        if (tataUser) {
            finish()
            overridePendingTransition(R.anim.slide_from_top, R.anim.slide_in_top);
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun pendingHraUpdation() {
        runOnUiThread {
            binding.webview.evaluateJavascript(
                "window.updateHraToAig()",
                null
            )
        }
    }

    override fun hraInComplete(jsonObject: String?, isIncomplete: Boolean) {
        jsonObject?.let {
            sharedPrefUtil.setHRAIncompleteStatusRequest(jsonObject);
            sharedPrefUtil.setHRAIncompleteStatus(isIncomplete)
        }
    }


    fun openCustomTab(activity: Activity, customTabsIntent: CustomTabsIntent, uri: Uri) {
        val packageName = "com.android.chrome"
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(activity, uri)
    }

    override fun consultationBooked() {
        runOnUiThread {

        }
    }


}




