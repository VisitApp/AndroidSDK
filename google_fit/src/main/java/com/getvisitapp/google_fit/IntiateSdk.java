package com.getvisitapp.google_fit;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.activity.SdkWebviewActivity;


@Keep
public class IntiateSdk {

    public static void s(Context c, boolean isDebug, String magicLink, String baseUrl, String default_client_id) {

        Intent intent = SdkWebviewActivity.Companion.getIntent(c, isDebug, magicLink, baseUrl, default_client_id);
        c.startActivity(intent);

    }
}
