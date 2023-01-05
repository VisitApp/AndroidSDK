package com.getvisitapp.google_fit.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatActivity
import com.getvisitapp.google_fit.util.Constants
import com.getvisitapp.google_fit.util.Constants.HRA_INCOMPLETE_RESPONSE
import com.getvisitapp.google_fit.util.Constants.IS_HRA_INCOMPLETE

@Keep
class SharedPrefUtil(context: Context) {

    var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("VisitSdkPref", AppCompatActivity.MODE_PRIVATE)
    var sharedPreferencesEditor: SharedPreferences.Editor = sharedPreferences.edit()


    fun setVisitBaseUrl(baseUrl: String) {
        sharedPreferencesEditor.putString(Constants.VISIT_BASE_URL, baseUrl).commit()
    }

    fun getVisitBaseUrl(): String {
        return sharedPreferences.getString(Constants.VISIT_BASE_URL, "")!!;
    }

    fun setTATA_AIG_MemberId(memberId: String) {
        sharedPreferencesEditor.putString(Constants.TATA_AIG_MEMBER_ID, memberId).commit()
    }

    fun getTATA_AIG_MemberId(): String {
        return sharedPreferences.getString(Constants.TATA_AIG_MEMBER_ID, "")!!
    }

    fun setVisitAuthToken(authToken: String) {
        sharedPreferencesEditor.putString(Constants.VISIT_AUTH_TOKEN, authToken).commit()
    }

    fun getVisitAuthToken(): String {
        return sharedPreferences.getString(Constants.VISIT_AUTH_TOKEN, "")!!
    }


    fun setTataAIGLastSyncTimeStamp(tata_aig_last_sync_time_stamp: Long) {
        sharedPreferencesEditor.putLong(
            Constants.TATA_AIG_LAST_SYNC_TIME_STAMP,
            tata_aig_last_sync_time_stamp
        ).commit()
    }

    fun getTataAIGLastSyncTimeStamp(): Long {
        return sharedPreferences.getLong(Constants.TATA_AIG_LAST_SYNC_TIME_STAMP, 0L)
    }

    fun setGoogleFitDailyLastSyncTimeStamp(google_fit_last_sync_daily_timestamp: Long) {
        sharedPreferencesEditor.putLong(
            Constants.GOOGLE_FIT_LAST_SYNC_DAILY_TIMESTAMP,
            google_fit_last_sync_daily_timestamp
        ).commit()
    }

    fun getGoogleFitDailyLastSyncTimeStamp(): Long {
        return sharedPreferences.getLong(Constants.GOOGLE_FIT_LAST_SYNC_DAILY_TIMESTAMP, 0L)
    }

    fun setGoogleFitHourlyLastSyncTimeStamp(google_fit_last_sync_hourly_timestamp: Long) {
        sharedPreferencesEditor.putLong(
            Constants.GOOGLE_FIT_LAST_SYNC_HOURLY_TIMESTAMP,
            google_fit_last_sync_hourly_timestamp
        ).commit()
    }

    fun getGoogleFitHourlyLastSyncTimeStamp(): Long {
        return sharedPreferences.getLong(Constants.GOOGLE_FIT_LAST_SYNC_HOURLY_TIMESTAMP, 0L)
    }

    fun setHRAIncompleteStatusRequest(jsonObject: String) {
        sharedPreferencesEditor.putString(HRA_INCOMPLETE_RESPONSE, jsonObject).commit();
    }

    fun getHRAInCompleteStatusResponse(): String? {
        return sharedPreferences.getString(HRA_INCOMPLETE_RESPONSE, null);
    }

    fun setHRAIncompleteStatus(isInComplete: Boolean) {
        sharedPreferencesEditor.putBoolean(IS_HRA_INCOMPLETE, isInComplete).commit()
    }

    fun getHRAIncompleteStatus(): Boolean {
        return sharedPreferences.getBoolean(IS_HRA_INCOMPLETE, true)
    }



}