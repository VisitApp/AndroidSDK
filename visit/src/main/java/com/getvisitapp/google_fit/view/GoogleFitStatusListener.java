package com.getvisitapp.google_fit.view;

import androidx.annotation.Keep;

@Keep
public interface GoogleFitStatusListener {

    void openLink(String url);

    void closeView();

    void askForLocationPermission();

}
