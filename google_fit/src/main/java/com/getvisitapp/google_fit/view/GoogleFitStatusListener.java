package com.getvisitapp.google_fit.view;

import androidx.annotation.Keep;

@Keep
public interface GoogleFitStatusListener {
    void connectToGoogleFit(boolean redirectUserToGoogleFitStatusPage);

    void disconnectFromGoogleFit();

    void connectToFitbit(String url,String authToken);

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

    void closeView(boolean tataUser);

    void pendingHraUpdation();

    void hraInComplete(String jsonObject, boolean isIncomplete);

    void consultationBooked();

    void loadDailyFitnessData(long steps,long sleep);



}
