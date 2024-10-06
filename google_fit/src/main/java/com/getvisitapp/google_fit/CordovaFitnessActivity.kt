package com.getvisitapp.google_fit;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.getvisitapp.google_fit.data.GoogleFitStatusListener;
import com.getvisitapp.google_fit.data.GoogleFitUtil;


public class CordovaFitnessActivity extends AppCompatActivity implements GoogleFitStatusListener {

    protected static final String TAG = "mytag";

    private ValueCallback<Uri[]> mUploadCallback;
    private final static int FILECHOOSER_REQUESTCODE = 1;

    GoogleFitUtil googleFitUtil;
    Activity activity;


    public static final String ACTIVITY_RECOGNITION = Manifest.permission.ACTIVITY_RECOGNITION;
    public static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    public static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 490;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 787;

    boolean dailyDataSynced = false;
    boolean syncDataWithServer = false;
    WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cordova_fitness);

        String magicLink = getIntent().getStringExtra("ssoLink");
        String default_client_id = getIntent().getStringExtra("default_client_id");


        Log.d(TAG, "CordovaFitnessActivity magicLink: " + magicLink);
        Log.d(TAG, "CordovaFitnessActivity default_client_id: " + default_client_id);


        activity = (Activity) this;

        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBuiltInZoomControls(false);
        settings.setGeolocationEnabled(true);

        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setDomStorageEnabled(true);

        // Multiple Windows set to true to mitigate Chromium security bug.
        // See: https://bugs.chromium.org/p/chromium/issues/detail?id=1083819
        settings.setSupportMultipleWindows(true);
        webView.requestFocus();
        webView.requestFocusFromTouch();


        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);


        // Enable Thirdparty Cookies
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture,
                                          Message resultMsg) {
                Log.d("mytag", "InAppChromeClient onCreateWindow");

                WebView inAppWebView = view;
                final WebViewClient webViewClient = new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        inAppWebView.loadUrl(request.getUrl().toString());
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        inAppWebView.loadUrl(url);
                        return true;
                    }

                };

                final WebView newWebView = new WebView(view.getContext());
                newWebView.setWebViewClient(webViewClient);

                final WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();

                return true;
            }

            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                Log.d(TAG, "File Chooser 5.0+");
                // If callback exists, finish it.
                if (mUploadCallback != null) {
                    mUploadCallback.onReceiveValue(null);
                }
                mUploadCallback = filePathCallback;

                // Create File Chooser Intent
                Intent content = new Intent(Intent.ACTION_GET_CONTENT);
                content.addCategory(Intent.CATEGORY_OPENABLE);
                content.setType("*/*");

                // Run cordova startActivityForResult
                startActivityForResult(
                        Intent.createChooser(content, "Select File"), FILECHOOSER_REQUESTCODE);
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                                                           GeolocationPermissions.Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                callback.invoke(origin, true, false);
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
                return shouldOverrideUrlLoading(request.getUrl().toString(), request.getMethod());
            }


            public boolean shouldOverrideUrlLoading(String url, String method) {
                boolean override = false;

                if (url.startsWith(WebView.SCHEME_TEL)) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        override = true;
                    } catch (android.content.ActivityNotFoundException e) {
                        Log.e(TAG, "Error dialing " + url + ": " + e.toString());
                    }
                } else if (url.startsWith("geo:") || url.startsWith(WebView.SCHEME_MAILTO) || url.startsWith("market:")
                        || url.startsWith("intent:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                        override = true;
                    } catch (android.content.ActivityNotFoundException e) {
                        Log.e(TAG, "Error with " + url + ": " + e.toString());
                    }
                }
                // If sms:5551212?body=This is the message
                else if (url.startsWith("sms:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);

                        // Get address
                        String address = null;
                        int parmIndex = url.indexOf('?');
                        if (parmIndex == -1) {
                            address = url.substring(4);
                        } else {
                            address = url.substring(4, parmIndex);

                            // If body, then set sms body
                            Uri uri = Uri.parse(url);
                            String query = uri.getQuery();
                            if (query != null) {
                                if (query.startsWith("body=")) {
                                    intent.putExtra("sms_body", query.substring(5));
                                }
                            }
                        }
                        intent.setData(Uri.parse("sms:" + address));
                        intent.putExtra("address", address);
                        intent.setType("vnd.android-dir/mms-sms");
                        startActivity(intent);
                        override = true;
                    } catch (android.content.ActivityNotFoundException e) {
                        Log.e(TAG, "Error sending sms " + url + ":" + e.toString());
                    }
                }
                return override;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(request.getUrl().toString(), super.shouldInterceptRequest(view, request),
                        request.getMethod());
            }

            public WebResourceResponse shouldInterceptRequest(String url, WebResourceResponse response, String method) {
                return response;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                String newloc = "";
                if (url.startsWith("http:") || url.startsWith("https:") || url.startsWith("file:")) {
                    newloc = url;
                } else {
                    // Assume that everything is HTTP at this point, because if we don't specify,
                    // it really should be. Complain loudly about this!!!
                    Log.e(TAG, "Possible Uncaught/Unknown URI");
                    newloc = "http://" + url;
                }

            }

            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // CB-10395 InAppBrowser's WebView not storing cookies reliable to local device
                // storage
                CookieManager.getInstance().flush();

                // https://issues.apache.org/jira/browse/CB-11248
                view.clearFocus();
                view.requestFocus();

            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);

            }
        });


        // 1. set background color
        webView.setBackgroundColor(Color.parseColor("#FFFFFF"));

        // // 2. add downloadlistener
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                        String mimetype, long contentLength) {

                Log.d("mytag", "DownloadListener called");

                try {
                    Uri uri = Uri.parse(url);
                    webView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        googleFitUtil = new GoogleFitUtil(activity, this, default_client_id, false);
        webView.addJavascriptInterface(googleFitUtil.getWebAppInterface(), "Android");
        googleFitUtil.init();

        webView.loadUrl(magicLink);


    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }


    /**
     * Receive File Data from File Chooser
     *
     * @param requestCode the requested code from chromeclient
     * @param resultCode  the result code returned from android system
     * @param intent      the data from android file chooser
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "CordovaFitnessPlugin onActivityResult called. requestCode: " + requestCode + " resultCode: "
                + resultCode);

        // If RequestCode or Callback is Invalid

        if (requestCode == 4097 || requestCode == 1900) {
            if (resultCode == Activity.RESULT_OK) {
                googleFitUtil.onActivityResult(requestCode, resultCode, intent);

            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
        }

        if (requestCode != FILECHOOSER_REQUESTCODE || mUploadCallback == null) {
            super.onActivityResult(requestCode, resultCode, intent);
            return;
        }
        if (mUploadCallback != null) {
            mUploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
        }
        mUploadCallback = null;

    }

    /**
     * This get called from the webview when user taps on [Connect To Google Fit]
     */

    @Override
    public void askForPermissions() {
        if (dailyDataSynced) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
        } else {
            googleFitUtil.askForGoogleFitPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case ACTIVITY_RECOGNITION_REQUEST_CODE:
                Log.d(TAG, "ACTIVITY_RECOGNITION_REQUEST_CODE permission granted");

                googleFitUtil.askForGoogleFitPermission();

                break;
            case LOCATION_PERMISSION_REQUEST_CODE:
                break;
        }
    }

    /**
     * 1A
     * This get called after user has granted all the fitness permission
     */

    @Override
    public void onFitnessPermissionGranted() {
        Log.d(TAG, "onFitnessPermissionGranted() called");

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                googleFitUtil.fetchDataFromFit();
            }
        });
    }

    /**
     * 1B
     * This is used to load the Daily Fitness Data into the Home Tab webView.
     */

    @Override
    public void loadDailyFitnessData(long steps, long sleep) {
        String finalString = "window.updateFitnessPermissions(true," + steps + "," +
                sleep + ")";

        webView.evaluateJavascript(
                finalString,
                null);
        dailyDataSynced = true;
    }

    /**
     * 2A
     * This get used for requesting data that are to be shown in detailed graph
     */


    @Override
    public void requestActivityData(String type, String frequency, long timestamp) {
        Log.d(TAG, "requestActivityData() called.");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (type != null && frequency != null) {
                    googleFitUtil.getActivityData(type, frequency, timestamp);
                }
            }
        });
    }

    /**
     * 2B
     * This get called when google fit return the detailed graph data that was
     * requested previously
     */

    @Override
    public void loadGraphData(String url) {
        Log.d("mytag", "detailed graph data: " + url);
        webView.evaluateJavascript(
                url,
                null);

    }

    @Override
    public void onFitnessPermissionCancelled() {
        Log.d("mytag", "onFitnessPermissionCancelled()");
    }

    @Override
    public void onFitnessPermissionDenied() {
        Log.d("mytag", "onFitnessPermissionDenied()");
    }


    @Override
    public void syncDataWithServer(String baseUrl, String authToken, long googleFitLastSync, long gfHourlyLastSync) {
        if (!syncDataWithServer) {
            Log.d(TAG, "syncDataWithServer() called");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    googleFitUtil.sendDataToServer(baseUrl + "/", authToken, googleFitLastSync, gfHourlyLastSync);
                    syncDataWithServer = true;
                }
            });
        }
    }

    @Override
    public void askForLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{LOCATION_PERMISSION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void closeVisitPWA() {
        finish();
    }

    @Override
    public void setDailyFitnessDataJSON(String s) {
        // not required
    }

    @Override
    public void setHourlyFitnessDataJSON(String s) {
        // not required
    }

    @Override
    public void onBackPressed() {
        if (this.webView.canGoBack()) {
            this.webView.goBack();
        } else {
            finish();
        }
    }
}