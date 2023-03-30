package com.getvisitapp.google_fit.okhttp;

import android.util.Log;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import rx.Observable;
import rx.functions.Func1;

public class MainActivityPresenter {
    private final String baseUrl;
    private final String authToken;
    OkHttpRequests okHttpRequests;
    boolean isLoggingEnabled;

    public MainActivityPresenter(String baseUrl, String authToken, boolean isLoggingEnabled) {
        this.baseUrl = baseUrl;
        this.authToken = authToken;
        this.okHttpRequests = new OkHttpRequests(authToken);
        this.isLoggingEnabled = isLoggingEnabled;
    }

    public Observable<ApiResponse> sendData(JsonObject payload) {
        if (isLoggingEnabled) {
            Log.d("mytag", "sendData() authToken: " + authToken);
        }

        String url = baseUrl + "users/data-sync";

        if (isLoggingEnabled) {
            Log.d("mytag", "dataSync URL: " + url);

        }
        return okHttpRequests.postRequest(url, payload, ApiResponse.class);
    }


    public Observable<Boolean> syncDayWithServer(JSONObject payload) {
        if (isLoggingEnabled) {
            Log.d("mytag", "syncDayWithServer: " + payload.toString());

        }
        String url = baseUrl + "users/embellish-sync";

        return okHttpRequests.postRequestHandler(url, payload, "SYNC_FITNESS")
                .concatMap(new Func1<JSONObject, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(JSONObject jsonObject) {
                        // If the server returns a positive response
                        if (isLoggingEnabled) {
                            Log.d("mytag", "call: API CALL SUCCESSFULL" + jsonObject);
                        }
                        try {
                            if (jsonObject.getString("message").equalsIgnoreCase("success")) {
                                return Observable.just(true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return Observable.just(false);
                    }
                });
    }
}
