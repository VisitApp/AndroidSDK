package com.getvisitapp.visit.view;

import androidx.annotation.Keep;

@Keep
public interface GoogleFitStatusListener {

    void openLink(String url);

    void closeView();

    void visitCallback(String jsonObject);

    void errorCallback(String jsonObject);

    void requestPermission(String type);

    default String getAppList(String requestUri) {
        return "[]";
    }

    /** Cashfree acknowledgement only; payment verification remains with the web checkout. */
    default boolean openApp(String upiClientPackage, String upiUri) {
        return true;
    }


}
