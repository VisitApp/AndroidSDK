package com.getvisitapp.google_fit.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

internal object UpiPaymentHelper {

    private const val TAG = "UpiPaymentHelper"

    @JvmStatic
    fun getAppList(context: Context, requestUri: String?): String {
        val apps = JSONArray()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUri))
            val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
            for (resolveInfo in resolveInfos) {
                val app = JSONObject().apply {
                    put(
                        "appName",
                        context.packageManager
                            .getApplicationLabel(resolveInfo.activityInfo.applicationInfo)
                            .toString()
                    )
                    put("appPackage", resolveInfo.activityInfo.packageName)
                }
                apps.put(app)
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Unable to get UPI app list", exception)
        }
        return apps.toString()
    }

    @JvmStatic
    fun openApp(
        activity: Activity,
        packageName: String?,
        upiUri: String?,
        requestCode: Int
    ): Boolean {
        try {
            val intent = resolveUpiIntent(activity, packageName, upiUri)
            if (intent != null) {
                activity.runOnUiThread {
                    try {
                        activity.startActivityForResult(intent, requestCode)
                    } catch (exception: Exception) {
                        Log.d(TAG, "Unable to launch UPI app", exception)
                    }
                }
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Unable to open UPI app", exception)
        }
        return true
    }

    private fun resolveUpiIntent(
        context: Context,
        packageName: String?,
        upiUri: String?
    ): Intent? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
        val resolveInfo = context.packageManager
            .queryIntentActivities(intent, 0)
            .firstOrNull { it.activityInfo.packageName == packageName }
            ?: return null

        return intent.setClassName(
            resolveInfo.activityInfo.packageName,
            resolveInfo.activityInfo.name
        )
    }
}
