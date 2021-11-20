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

class WebViewActivity : AppCompatActivity(), AdvancedWebView.Listener, GoogleFitStatusListener {

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
        mWebView.loadUrl("https://web.getvisitapp.xyz/");
        var webAppInterface = WebAppInterface(this)
        webAppInterface.listener = this
        mWebView.addJavascriptInterface(webAppInterface, "Android")

        WebView.setWebContentsDebuggingEnabled(true);

        stepsCounter = StepsCounter.getInstance(this)

        googleFitConnector = GoogleFitConnector(
            this,
            this.getString(R.string.default_web_client_id),
            object : GoogleFitConnector.GoogleConnectorFitListener {
                override fun onComplete() {
                    Log.d(TAG, "onComplete() called")
                }

                override fun onError() {
                    Log.d(TAG, "onError() called")

                }

                override fun onServerAuthCodeFound(p0: String?) {
                    Log.d(TAG, "error Occured: $p0")
                }

            })

        if(stepsCounter.hasAccess()){
            googleFitConnector.getTotalStepsForToday()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : Subscriber<List<Int?>?>() {
                    override fun onCompleted() {}
                    override fun onError(e: Throwable) {
                        e.printStackTrace()
                    }



                    override fun onNext(t: List<Int?>?) {
                        Log.d("mytag","${t!![0]}")

                        mWebView.evaluateJavascript("window.googleFitPermissionGranted(true, '2000', '320')", null)

                    }


                })
        }





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

    override fun onPageStarted(url: String?, favicon: Bitmap?) {}

    override fun onPageFinished(url: String?) {
        Log.d(TAG,"onPageFinished: $url")

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
            stepsCounter!!.onActivityResult(requestCode, resultCode, data)
        }
        mWebView.onActivityResult(requestCode, resultCode, intent);

    }

    override fun onBackPressed() {
        if (!mWebView.onBackPressed()) {
            return; }
        super.onBackPressed()

    }


    class WebAppInterface(private val mContext: Context) {
        var TAG = this.javaClass.simpleName
        var listener: GoogleFitStatusListener? = null


        @JavascriptInterface
        fun connectToGoogleFit() {
            Log.d(TAG, "connectToGoogleFit() called")

            listener?.askForGoogleFitPermission()

        }
    }


    override fun askForGoogleFitPermission() {

        stepsCounter!!.run(
            this.getString(R.string.default_web_client_id),
            GenericListener {
                Log.d(TAG, "Job Done: $it");

                runOnUiThread {
                     mWebView.evaluateJavascript("window.googleFitPermissionGranted(true, '2000', '320')", null)
                }

            })
    }


}

interface GoogleFitStatusListener {
    fun askForGoogleFitPermission()
}
