package com.getvisitapp.visit.data;

public interface GoogleFitStatusListener {
    void askForPermissions();

    void requestActivityData(String type, String frequency, long timestamp);

    void syncDataWithServer(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync);

    void getHealthConnectStatus();
}
