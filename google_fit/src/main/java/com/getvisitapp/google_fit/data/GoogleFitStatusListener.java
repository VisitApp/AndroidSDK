package com.getvisitapp.google_fit.data;

public interface GoogleFitStatusListener {
    void askForPermissions();
    void onFitnessPermissionGranted();
    void loadWebUrl(String urlString);
    void requestActivityData(String type, String frequency, long timestamp);
    void loadGraphDataUrl(String url);
}
