package com.getvisitapp.google_fit.data;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class WebAppInterface {
    GoogleFitStatusListener listener;

    public WebAppInterface(GoogleFitStatusListener listener) {
        this.listener = listener;
    }

    @JavascriptInterface
    public void connectToGoogleFit() {

        Log.d("mytag", "connectToGoogleFit() called");

        listener.askForPermissions();

    }

    @JavascriptInterface
    public void getDataToGenerateGraph(String type, String frequency, long timestamp) {
        Log.d("mytag", "getDataToGenerateGraph() called. type:" + type + " frequency: " + frequency + " timestamp:" + timestamp);
        listener.requestActivityData(type, frequency, timestamp);
    }

    @JavascriptInterface
    public void updateApiBaseUrl(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync) {
        Log.d("mytag", "updateApiBaseUrl() called. apiBaseUrl: " + apiBaseUrl + ",authtoken: " + authtoken + ",googleFitLastSync: " + googleFitLastSync + ",gfHourlyLastSync: " + gfHourlyLastSync);
        listener.syncDataWithServer(apiBaseUrl, authtoken, googleFitLastSync, gfHourlyLastSync);
    }

    @JavascriptInterface
    public void getLocationPermissions(){
        Log.d("mytag", "getLocationPermissions() called.");
        listener.askForLocationPermission();

    }

    @JavascriptInterface
    public void closeView(){
        Log.d("mytag", "closeView() called.");
        listener.closeVisitPWA();
    }
}