package com.getvisitapp.google_fit.data;

public interface GoogleFitStatusListener {
    void askForPermissions();
    void onFitnessPermissionGranted();
    void onFitnessPermissionCancelled();
    void onFitnessPermissionDenied();
    void loadWebUrl(String urlString);
    void requestActivityData(String type, String frequency, long timestamp);
    void loadGraphDataUrl(String url);
    void syncDataWithServer(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync);
    void askForLocationPermission();
    void closeVisitPWA();
}