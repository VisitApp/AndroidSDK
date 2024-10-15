package com.getvisitapp.google_fit.view;

import androidx.annotation.Keep;

@Keep
public interface GoogleFitStatusListener {

    void openLink(String url);

    void startVideoCall(int sessionId, int consultationId, String authToken);

    void closeView();

    void askForLocationPermission();

}
