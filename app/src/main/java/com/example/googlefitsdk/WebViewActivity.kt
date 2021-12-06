package com.example.googlefitsdk

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.getvisitapp.google_fit.GenericListener
import com.getvisitapp.google_fit.GoogleFitConnector
import com.getvisitapp.google_fit.StepsCounter
import im.delight.android.webview.AdvancedWebView
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class WebViewActivity : AppCompatActivity(), AdvancedWebView.Listener, GoogleFitStatusListener,
    GenericListener {

    var TAG = this.javaClass.simpleName

    lateinit var mWebView: AdvancedWebView
    lateinit var stepsCounter: StepsCounter
    lateinit var googleFitConnector: GoogleFitConnector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        mWebView = findViewById(R.id.webview);
        mWebView.setListener(this, this);
        mWebView.setMixedContentAllowed(false);
        mWebView.settings.javaScriptEnabled = true

        val baseUrl: String = "https://star-health.getvisitapp.xyz/star-health"
        val authToken: String = "Bearer%20eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOi[%E2%80%A6]GFsIn0.f0656mzmcRMSCywkbEptdd6JgkDfIqN0S9t-P1aPyt8"
        val userId: String = "8158"

        val magicLink = "$baseUrl?token=$authToken&id=$userId"
        Log.d(TAG, "magicLink: $magicLink")


        mWebView.loadUrl(magicLink);


//        var webAppInterface = WebAppInterface(this)
//
//        mWebView.addJavascriptInterface(webAppInterface, "Android")
//
//        WebView.setWebContentsDebuggingEnabled(true);
//
//        stepsCounter = StepsCounter.getInstance(this)
//
//        googleFitConnector = GoogleFitConnector(
//            this,
//            this.getString(R.string.default_web_client_id),
//            object : GoogleFitConnector.GoogleConnectorFitListener {
//                override fun onComplete() {
//                    Log.d(TAG, "onComplete() called")
//                }
//
//                override fun onError() {
//                    Log.d(TAG, "onError() called")
//
//                }
//
//                override fun onServerAuthCodeFound(p0: String?) {
//                    Log.d(TAG, "error Occured: $p0")
//                }
//
//            })
//
//        if (stepsCounter.hasAccess()) {
//            googleFitConnector.getTotalStepsForToday()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(object : Subscriber<List<Int?>?>() {
//                    override fun onCompleted() {}
//                    override fun onError(e: Throwable) {
//                        e.printStackTrace()
//                    }
//
//
//                    override fun onNext(t: List<Int?>?) {
//                        Log.d("mytag", "${t!![0]}")
//
//                        mWebView.evaluateJavascript(
//                            "window.googleFitPermissionGranted(true, '2000', '320')",
//                            null
//                        )
//
//                    }
//
//
//                })
//        }


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
        if (googleFitConnector != null) {
            googleFitConnector.onActivityResult(requestCode, resultCode, data)
        }
        if (stepsCounter != null) {
            stepsCounter!!.onActivityResult(requestCode, resultCode, data,this)
        }
        mWebView.onActivityResult(requestCode, resultCode, intent);

    }

    override fun onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return; }
        super.onBackPressed()

    }


    class WebAppInterface(private var listener: GoogleFitStatusListener) {

        @JavascriptInterface
        fun connectToGoogleFit() {
            Log.d("mytag", "connectToGoogleFit() called")

            listener?.askForGoogleFitPermission()

        }

        @JavascriptInterface
        fun getDataToGenerateGraph(type: String, frequency: String, timestamp: Long) {
            Log.d(
                "mytag",
                "getDataToGenerateGraph() called. type:$type frequency: $frequency timestamp:$timestamp"
            )
            listener.setGraphData(type, frequency, timestamp)
        }
    }


    override fun askForGoogleFitPermission() {

        stepsCounter!!.run(
            this.getString(R.string.default_web_client_id),
            GenericListener {
                Log.d(TAG, "Job Done: $it");

                runOnUiThread {
                    mWebView.evaluateJavascript(
                        "window.googleFitPermissionGranted(true, '2000', '320')",
                        null
                    )
                }

            })
    }

    override fun setGraphData(type: String?, frequency: String?, timestamp: Long) {
        Log.d("mytag", "updateData() called")
        runOnUiThread {
            frequency?.let {
                when (frequency) {
                    "day" -> {
                        mWebView.evaluateJavascript(
                            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],[100,200,300], 'steps', 'day')",
                            null
                        )
                    }
                    "week" -> {
                        mWebView.evaluateJavascript(
                            "DetailedGraph.updateData([1,2,3,4,5,6,7],[100,200,300,10000], 'steps', 'week')",
                            null
                        )
                    }
                    "month" -> {
                        mWebView.evaluateJavascript(
                            "DetailedGraph.updateData([1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31],[100,200,300,1000,400,6000], 'steps', 'month')",
                            null
                        )
                    }
                    else -> {

                    }
                }
            }


        }
    }

    override fun onJobDone(email: String?) {
        if(email!=null){
            if(email=="UPDATE THE UI"){
                println()
            }
        }
    }


}

interface GoogleFitStatusListener {
    fun askForGoogleFitPermission()
    fun setGraphData(type: String?, frequency: String?, timestamp: Long)
}
