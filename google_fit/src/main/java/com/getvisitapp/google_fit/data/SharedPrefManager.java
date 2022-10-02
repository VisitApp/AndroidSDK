package com.getvisitapp.google_fit.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final String CALORIE_COUNT_KEY = "calorie_count_key";

    SharedPrefManager(Context context) {
        sharedPreferences = context.getSharedPreferences("VisitSDK", 0);
        editor = sharedPreferences.edit();
    }

    public void setCalorieCount(double count) {
        editor.putFloat(CALORIE_COUNT_KEY, (float) count).commit();
    }

    public float getCalorieCount() {
        return sharedPreferences.getFloat(CALORIE_COUNT_KEY, -1f);
    }
}
