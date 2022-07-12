package com.getvisitapp.google_fit.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.getvisitapp.google_fit.pojo.HraInCompleteResponse
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.getvisitapp.google_fit.util.GoogleFitConnector
import com.getvisitapp.google_fit.util.GoogleFitConnector.GoogleConnectorFitListener
import com.google.gson.Gson
import org.json.JSONObject

@Keep
class VisitStepSyncHelper(var context: Context, var default_web_client_id: String) {

    private var TAG = "mytag1"
    private var sharedPrefUtil = SharedPrefUtil(context)
    private lateinit var syncStepHelper: SyncStepHelper
    private lateinit var checker: GoogleFitAccessChecker

    fun syncSteps(
        tataAIG_base_url: String,
        tata_aig_authToken: String,
        timeStamp: Long = 0L //For testing purpose, pass the timestamp value
    ) {

        checker = GoogleFitAccessChecker(context)

        if (checker.checkGoogleFitAccess()) {

            val tataAIGLastSyncTimeStamp = if (timeStamp == 0L)
                sharedPrefUtil.getTataAIGLastSyncTimeStamp()
            else timeStamp

            val baseUrl = sharedPrefUtil.getVisitBaseUrl()
            val authToken = sharedPrefUtil.getVisitAuthToken()
            val memberId = sharedPrefUtil.getTATA_AIG_MemberId()

            if (tataAIGLastSyncTimeStamp != 0L) {

                val googleFitConnector =
                    GoogleFitConnector(
                        context,
                        default_web_client_id,
                        object : GoogleConnectorFitListener {
                            override fun onComplete() {
                                Log.d(TAG, "onComplete() called")
                            }

                            override fun onError() {
                                Log.d(TAG, "onError() called")
                            }

                            override fun onServerAuthCodeFound(s: String) {
                                Log.d(TAG, "error Occured: $s")
                            }
                        })


                syncStepHelper = SyncStepHelper(
                    googleFitConnector,
                    baseUrl,
                    authToken,
                    tataAIG_base_url,
                    tata_aig_authToken,
                    memberId,
                    context
                )

            }

            if (tataAIGLastSyncTimeStamp != 0L) {
                syncStepHelper.hourlySync(tataAIGLastSyncTimeStamp, true)
            }
        }


    }

    fun sendHRAInComplete(
        tataAIG_base_url: String,
        tata_aig_authToken: String
    ) {


        val baseUrl = sharedPrefUtil.getVisitBaseUrl()
        val authToken = sharedPrefUtil.getVisitAuthToken()
        val memberId = sharedPrefUtil.getTATA_AIG_MemberId()

        val hraInCompleteStatusResponse = sharedPrefUtil.getHRAInCompleteStatusResponse()
        val isHraInComplete: Boolean = sharedPrefUtil.getHRAIncompleteStatus()

        if (!hraInCompleteStatusResponse.isNullOrEmpty() && isHraInComplete==false) {
            val googleFitConnector =
                GoogleFitConnector(
                    context,
                    default_web_client_id,
                    object : GoogleConnectorFitListener {
                        override fun onComplete() {
                            Log.d(TAG, "onComplete() called")
                        }

                        override fun onError() {
                            Log.d(TAG, "onError() called")
                        }

                        override fun onServerAuthCodeFound(s: String) {
                            Log.d(TAG, "error Occured: $s")
                        }
                    })

            syncStepHelper = SyncStepHelper(
                googleFitConnector,
                baseUrl,
                authToken,
                tataAIG_base_url,
                tata_aig_authToken,
                memberId,
                context
            )

            try {

                val hraInCompleteResponse: HraInCompleteResponse = Gson().fromJson(
                    hraInCompleteStatusResponse,
                    HraInCompleteResponse::class.java
                )

                val jsonObject = JSONObject()
                jsonObject.put("member_id", hraInCompleteResponse.member_id)

                val hraDetails = JSONObject()
                hraDetails.put("color", hraInCompleteResponse.hra_details.color)
                hraDetails.put("score", hraInCompleteResponse.hra_details.score)

                jsonObject.put("hra_details", hraDetails);

                syncStepHelper.sendHRAInCompleteStatusToTataAIG(jsonObject)

                Log.d("mytag", "VisitStepSyncHelper hraInCompleteResponse: $hraInCompleteResponse")
            } catch (e: Exception) {
                e.printStackTrace()
            }


        }

    }

    companion object {
        fun Context.openGoogleFit(): Boolean {
            val launchIntent =
                packageManager.getLaunchIntentForPackage("com.google.android.apps.fitness")
            return if (launchIntent != null) {
                startActivity(launchIntent)
                true;
            } else {
                false;
            }
        }
    }




}