package com.getvisitapp.google_fit.data;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.getvisitapp.google_fit.view.GoogleFitStatusListener;

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
    public void updateApiBaseUrl(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync, String memberId) {
        Log.d("mytag", "updateApiBaseUrl() called. apiBaseUrl: " + apiBaseUrl + ",authtoken: " + authtoken + ",googleFitLastSync: " + googleFitLastSync + ",gfHourlyLastSync: " + gfHourlyLastSync + " memberId: " + memberId);
        listener.syncDataWithServer(apiBaseUrl, authtoken, googleFitLastSync, gfHourlyLastSync, memberId);
    }

    @JavascriptInterface
    public void getLocationPermissions() {
        Log.d("mytag", "getLocationPermissions() called.");
        listener.askForLocationPermission();

    }

    @JavascriptInterface
    public void initiateVideoCall(int sessionId, int consultationId, String authToken) {
        Log.d("mytag", "initiateVideoCall() called.");
        listener.startVideoCall(sessionId, consultationId, authToken);
    }

    @JavascriptInterface
    public void hraCompleted() {
        Log.d("mytag", "hraCompleted() called.");
        listener.hraCompleted();
    }

    @JavascriptInterface
    public void googleFitConnectedAndSavedInPWA() {
        Log.d("mytag", "googleFitConnectedAndSavedInPWA() called.");
        listener.googleFitConnectedAndSavedInPWA();
    }

    @JavascriptInterface
    public void inHraEndPage() {
        Log.d("mytag", "inHraEndPage() called");
        listener.inHraEndPage();
    }

    @JavascriptInterface
    public void hraQuestionAnswered(int current, int total) {
        Log.d("mytag", "hraQuestionAnswered() called");
        listener.hraQuestionAnswered(current, total);
    }

    @JavascriptInterface
    public void downloadHraLink(String link) {
        Log.d("mytag", "downloadHraLink() called");
        listener.downloadHraLink(link);
    }

    @JavascriptInterface
    public void inFitSelectScreen() {
        Log.d("mytag", "inFitSelectScreen() called");
        listener.inFitSelectScreen();
    }


}