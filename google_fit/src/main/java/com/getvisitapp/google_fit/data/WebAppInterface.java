package com.getvisitapp.google_fit.data;

import android.webkit.JavascriptInterface;

import timber.log.Timber;

public class WebAppInterface {
    GoogleFitStatusListener listener;

    public WebAppInterface(GoogleFitStatusListener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void connectToGoogleFit() {

        Timber.d("mytag: connectToGoogleFit() called");

        listener.askForPermissions();

    }

    @JavascriptInterface
    public void getDataToGenerateGraph(String type, String frequency, long timestamp) {
        Timber.d("mytag: getDataToGenerateGraph() called. type:" + type + " frequency: " + frequency + " timestamp:" + timestamp);
        listener.requestActivityData(type, frequency, timestamp);
    }

    @JavascriptInterface
    public void updateApiBaseUrl(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync) {
        Timber.d("mytag: updateApiBaseUrl() called. apiBaseUrl: " + "NOT SHOWN" + ",authtoken: " + "NOT SHOWN" + ",googleFitLastSync: " + googleFitLastSync + ",gfHourlyLastSync: " + gfHourlyLastSync);
        listener.syncDataWithServer(apiBaseUrl, authtoken, googleFitLastSync, gfHourlyLastSync);
    }

    @JavascriptInterface
    public void getLocationPermissions() {
        Timber.d("mytag: getLocationPermissions() called.");
        listener.askForLocationPermission();

    }

    @JavascriptInterface
    public void closeView() {
        Timber.d("mytag: closeView() called.");
        listener.closeVisitPWA();
    }
}