package com.getvisitapp.visit.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil

import com.getvisitapp.visit.R
import com.getvisitapp.visit.databinding.PaymentWebViewBinding
import im.delight.android.webview.AdvancedWebView
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class PaymentActivity : AppCompatActivity(), AdvancedWebView.Listener, AfterPaymentListener {

    lateinit var binding: PaymentWebViewBinding
    lateinit var paymentUrl: String
    lateinit var token: String


    companion object {
        val PAYMENT_URL = "paymentUrl"
        val TOKEN = "token"

        fun getIntent(
            context: Context, paymentUrl: String, token: String
        ): Intent {
            val intent = Intent(context, PaymentActivity::class.java)
            intent.putExtra(PAYMENT_URL, paymentUrl)
            intent.putExtra(TOKEN, token)
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_payment_webview_scan_and_pay)


        paymentUrl = intent.getStringExtra(PAYMENT_URL)!!
        token = intent.getStringExtra(TOKEN)!!

//        Toast.makeText(this,orderDeliveryType,Toast.LENGTH_SHORT).show()

        binding.infoView.setVisibility(View.GONE)
        binding.webview.setListener(this, this)
        binding.webview.setGeolocationEnabled(false)
        binding.webview.setMixedContentAllowed(true)
        binding.webview.setCookiesEnabled(true)
        binding.webview.setThirdPartyCookiesEnabled(true)
        binding.webview.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
//                Toast.makeText(WVActivity.this, "Finished loading " + url, Toast.LENGTH_SHORT).show();
            }
        })
        val webAppInterface: WebAppInterface = WebAppInterface(this)
        webAppInterface.setListener(this)
        binding.webview.addJavascriptInterface(webAppInterface, "Android")

        val jsonObject = JSONObject()
        jsonObject.put("category", "medicine")
        jsonObject.put("screenName", this.javaClass.simpleName)

        binding.webview.setWebChromeClient(object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
                //                Toast.makeText(WVActivity.this, title, Toast.LENGTH_SHORT).show();
            }
        })



        binding.webview.addHttpHeader("Authorization", token)


        Timber.d("paymentUrl: $paymentUrl");
        binding.webview.loadUrl(paymentUrl)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {

                    Timber
                        .d("webview.canGoBack(): ${binding.webview.canGoBack()}, url: ${binding.webview.url}")

                    if (binding.webview.canGoBack()) {
                        binding.webview.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPageStarted(url: String?, favicon: Bitmap?) {
        binding.linearProgressIndicator.visibility = View.VISIBLE
    }

    override fun onPageFinished(url: String?) {
        binding.linearProgressIndicator.visibility = View.GONE
    }

    override fun onPageError(errorCode: Int, description: String?, failingUrl: String?) {

    }

    override fun onDownloadRequested(
        url: String?,
        suggestedFilename: String?,
        mimeType: String?,
        contentLength: Long,
        contentDisposition: String?,
        userAgent: String?
    ) {

    }

    override fun onExternalPageRequest(url: String?) {

    }

    override fun transactionFailure(jsonObject: JSONObject?) {

    }

    override fun transactionSuccess() {

    }


    class WebAppInterface
    /**
     * Instantiate the interface and set the context
     */ internal constructor(var mContext: Context) {
        private var listener: AfterPaymentListener? = null
        fun setListener(listener: AfterPaymentListener?) {
            this.listener = listener
        }

        @JavascriptInterface
        @Throws(JSONException::class)
        fun transcationSuccess() {
            Timber.tag("mytag").d(
                "transactionSuccess()"
            )
            listener!!.transactionSuccess()
        }

        @JavascriptInterface
        fun transactionFailure(response: String?, transactionId: String?) {
            Timber.tag("mytag").d(
                "transactionFailure()"
            )

            try {
                val jsonObject = JSONObject()
                jsonObject.put("message", response)
                listener!!.transactionFailure(jsonObject)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
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
}