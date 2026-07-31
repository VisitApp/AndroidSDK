package com.getvisitapp.visit.view;

import androidx.annotation.Keep;

@Keep
public interface GoogleFitStatusListener {

    void askForLocationPermission();

    void openLink(String url);

    void closeView();

    void visitCallback(String jsonObject);

    void errorCallback(String jsonObject);

    void requestPermission(String type);


}
