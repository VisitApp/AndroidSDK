package com.getvisitapp.google_fit.data;

import android.util.Log;
import android.webkit.JavascriptInterface;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.view.GoogleFitStatusListener;

@Keep
public class WebAppInterface {
    GoogleFitStatusListener listener;

    public WebAppInterface(GoogleFitStatusListener listener) {
        this.listener = listener;
    }


    @JavascriptInterface
    public void getLocationPermissions() {
        Log.d("mytag", "getLocationPermissions() called.");
        listener.askForLocationPermission();
    }



    @JavascriptInterface
    public void closeView() {
        Log.d("mytag", "closeView() called");
        listener.closeView();
    }


    @JavascriptInterface
    public void openLink(String url) {
        Log.d("mytag", "openLink called(). url:" + url);
        listener.openLink(url);
    }


}