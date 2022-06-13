package com.getvisitapp.google_fit.data;

public interface GoogleFitStatusListener {
    void askForPermissions();
    void onFitnessPermissionGranted();
    void loadGraphData(String data);
    void onFitnessPermissionCancelled();

    void onFitnessPermissionDenied();
    void requestActivityData(String type, String frequency, long timestamp);
    void loadDailyFitnessData(long steps,long sleep);

    void syncDataWithServer(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync);
    void askForLocationPermission();
    void closeVisitPWA();
}
