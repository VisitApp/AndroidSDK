package com.getvisitapp.visit.data;

import android.webkit.JavascriptInterface;

import androidx.annotation.Keep;

import com.getvisitapp.visit.view.GoogleFitStatusListener;

import timber.log.Timber;

@Keep
public class WebAppInterface {
    GoogleFitStatusListener listener;

    public WebAppInterface(GoogleFitStatusListener listener) {
        this.listener = listener;
    }


    @JavascriptInterface
    public void closeView() {
        Timber.tag("mytag")
                .d( "closeView() called");
        listener.closeView();
    }


    @JavascriptInterface
    public void openLink(String url) {
        Timber.tag("mytag")
                .d( "openLink called(). url:" + url);
        listener.openLink(url);
    }

    @JavascriptInterface
    public void visitCallback(String jsonObject) {
        Timber.tag("mytag")
                .d( "visitCallback()");
        listener.visitCallback(jsonObject);
    }

    @JavascriptInterface
    public void errorCallback(String jsonObject) {
        Timber.tag("mytag")
                .d( "errorCallback()");
        listener.errorCallback(jsonObject);
    }


}
