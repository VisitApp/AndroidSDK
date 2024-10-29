package com.getvisitapp.google_fit;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Keep;

import com.getvisitapp.google_fit.activity.SdkWebviewActivity;


@Keep
public class IntiateSdk {

    public static void s(Context c, boolean isDebug, String magicLink) {
        Intent intent = SdkWebviewActivity.Companion.getIntent(c, isDebug, magicLink);
        c.startActivity(intent);
    }
}
