package com.getvisitapp.google_fit.view;

public interface GoogleFitStatusListener {
    void askForPermissions();

    void onFitnessPermissionGranted();

    void loadWebUrl(String urlString);

    void requestActivityData(String type, String frequency, long timestamp);

    void loadGraphDataUrl(String url);

    void syncDataWithServer(String apiBaseUrl, String authtoken, long googleFitLastSync, long gfHourlyLastSync, String memberId);

    void askForLocationPermission();

    void startVideoCall(int sessionId, int consultationId, String authToken);

    void hraCompleted();

    void googleFitConnectedAndSavedInPWA();

    void inHraEndPage();

    void hraQuestionAnswered(int current, int total);

    void downloadHraLink(String link);

    void inFitSelectScreen();

    void openDependentLink(String link);
}
