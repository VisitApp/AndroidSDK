package com.getvisitapp.google_fit.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.getvisitapp.google_fit.model.SyncDateHelper
import com.getvisitapp.google_fit.model.TataAIGFitnessPayload
import com.getvisitapp.google_fit.network.APIServiceInstance
import com.getvisitapp.google_fit.network.ApiService
import com.getvisitapp.google_fit.pojo.HraInCompleteResponse
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.getvisitapp.google_fit.util.GoogleFitConnector
import com.getvisitapp.google_fit.util.GoogleFitConnector.GoogleConnectorFitListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Keep
class VisitStepSyncHelper(var context: Context, var default_web_client_id: String) {

    private var TAG = "mytag1"
    private var sharedPrefUtil = SharedPrefUtil(context)
    private lateinit var syncStepHelper: SyncStepHelper
    private lateinit var checker: GoogleFitAccessChecker

    fun syncSteps(
        tataAIG_base_url: String,
        tata_aig_authToken: String,
        startTimeStamp: Long = 0L //For testing purpose, pass the timestamp value
    ) {
        if (sharedPrefUtil.getFitBitConnectionStatus()) {
            syncFitbitSteps(
                tataAIG_base_url = tataAIG_base_url,
                tata_aig_authToken = tata_aig_authToken,
                startTimeStamp = startTimeStamp
            )
        } else {
            checker = GoogleFitAccessChecker(context)

            if (checker.checkGoogleFitAccess()) {

                val tataAIGLastSyncTimeStamp =
                    if (startTimeStamp == 0L) sharedPrefUtil.getTataAIGLastSyncTimeStamp()
                    else startTimeStamp

                val baseUrl = sharedPrefUtil.getVisitBaseUrl()
                val authToken = sharedPrefUtil.getVisitAuthToken()
                val memberId = sharedPrefUtil.getTATA_AIG_MemberId()

                if (tataAIGLastSyncTimeStamp != 0L) {

                    val googleFitConnector = GoogleFitConnector(
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
                        context,
                        null
                    )

                }

                if (tataAIGLastSyncTimeStamp != 0L) {
                    syncStepHelper.hourlySync(tataAIGLastSyncTimeStamp, -1, false, true)
                }
            }

        }


    }

    fun sendHRAInComplete(
        tataAIG_base_url: String, tata_aig_authToken: String
    ) {


        val baseUrl = sharedPrefUtil.getVisitBaseUrl()
        val authToken = sharedPrefUtil.getVisitAuthToken()
        val memberId = sharedPrefUtil.getTATA_AIG_MemberId()

        val hraInCompleteStatusResponse = sharedPrefUtil.getHRAInCompleteStatusResponse()
        val isHraInComplete: Boolean = sharedPrefUtil.getHRAIncompleteStatus()

        if (!hraInCompleteStatusResponse.isNullOrEmpty() && isHraInComplete == false) {
            val googleFitConnector = GoogleFitConnector(
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
                /* connector = */ googleFitConnector,
                /* baseUrl = */ baseUrl,
                /* authToken = */ authToken,
                /* tata_aig_baseURL = */ tataAIG_base_url,
                /* tata_aig_authToken = */ tata_aig_authToken,
                /* memberId = */ memberId,
                /* context = */ context,
                /* syncStatusListener = */ null
            )

            try {

                val hraInCompleteResponse: HraInCompleteResponse = Gson().fromJson(
                    hraInCompleteStatusResponse, HraInCompleteResponse::class.java
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

    fun getVisitApiService(baseUrl: String, visitAuthToken: String): ApiService {

        Log.d("mytag", "getVisitApiService authToken: $visitAuthToken, baseUrl: $baseUrl")

        return APIServiceInstance.getApiService(
            baseUrl, context, visitAuthToken, true
        )
    }

    fun getTataAigAPIService(
        tataAIG_base_url: String, tata_aig_authToken: String
    ): ApiService {

        Log.d(
            "mytag",
            "getTataAigAPIService authToken: $tata_aig_authToken, baseUrl: $tataAIG_base_url"
        )

        return APIServiceInstance.getApiService(
            tataAIG_base_url, context, tata_aig_authToken, true
        )
    }

    //pass the start time if the client application wants to sync some data custom data.
    fun syncFitbitSteps(
        tataAIG_base_url: String,
        tata_aig_authToken: String,
        startTimeStamp: Long = 0L,
        endTimeStamp: Long = 0L,
        isManual: Boolean = false
    ) {

        val startOfDay = if (startTimeStamp == 0L) {
            sharedPrefUtil.getFitbitLastSyncTimestamp()
        } else {
            startTimeStamp
        }

        val syncDateHelper = SyncDateHelper()
        val syncStartAndEndDate =
            syncDateHelper.getSyncDates(startOfDay, endTimeStamp, isManual)

        val policyNumber = sharedPrefUtil.getPolicyNumber()
        val readableFormat = SimpleDateFormat("d MMM, yyyy", Locale.ENGLISH)

        Log.d(
            TAG,
            "Start Time: " + readableFormat.format(syncStartAndEndDate.startTimeStamp) + " timestamp: " + syncStartAndEndDate.startTimeStamp
        )
        Log.d(
            TAG,
            "End Of day: " + readableFormat.format(syncStartAndEndDate.endTimeStamp) + " timestamp: " + syncStartAndEndDate.endTimeStamp
        )
        Log.d(TAG, "isManual: " + syncStartAndEndDate.isManual)


        val visitBaseUrl = sharedPrefUtil.getVisitBaseUrl()
        val visitAuthToken = sharedPrefUtil.getVisitAuthToken()


        val visitApiService = getVisitApiService(
            baseUrl = visitBaseUrl, visitAuthToken = visitAuthToken
        )

        val tataApiService = getTataAigAPIService(
            tataAIG_base_url = tataAIG_base_url, tata_aig_authToken = tata_aig_authToken
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fitbitStepsResponse = visitApiService.getFitBitStatus(
                    syncStartAndEndDate.startTimeStamp,
                    syncStartAndEndDate.endTimeStamp
                )

                if (fitbitStepsResponse.message == "success") {

                    sharedPrefUtil.setFitBitConnectedStatus(fitbitStepsResponse.status)

                    //status flag returns if the fitbit is connected or not.
                    if (fitbitStepsResponse.status == true) {

                        val requestBody = TataAIGFitnessPayload(
                            member_id = sharedPrefUtil.getTATA_AIG_MemberId(),
                            data = fitbitStepsResponse.data
                        )

                        val tataAIGServerResponse = tataApiService.pushDataToTataAIG(requestBody)
                        if (tataAIGServerResponse.has("action") && tataAIGServerResponse.get("action").asString == "SUCCESS") {
                            Log.d("mytag", "Fitbit data synced with TATA AIG server successfully")

                            //after data is successfully synced to tata aig, saved the last sync timestamp. last sync timestamp should be the current data set to 12AM.

                            //normalising the end date timestamp value
                            val endCalMinusOneDay: Calendar = Calendar.getInstance()
                            endCalMinusOneDay.timeInMillis = syncStartAndEndDate.endTimeStamp
                            endCalMinusOneDay.add(Calendar.DATE, -1)
                            val endOfDayMinusOneDayInMillis = endCalMinusOneDay.timeInMillis

                            Log.d(
                                "mytag",
                                "endCalMinusOneDay timestamp: " + endOfDayMinusOneDayInMillis
                            )

                            Log.d("mytag", "endCal timestamp: " + endCalMinusOneDay.timeInMillis)

                            //update the timestamp in for Visit Database.

                            val body = JsonObject()

                            body.addProperty("lastSyncTimeStamp", endOfDayMinusOneDayInMillis)
                            body.addProperty("policyNumber", policyNumber)

                            val response =
                                visitApiService.updateFitbitLastSyncTimestampForVisit(body)

                            if (response.status == "success") {
                                sharedPrefUtil.setFitBitLastSyncTimeStamp(
                                    endOfDayMinusOneDayInMillis
                                )
                                Log.d("mytag", "Fitbit timestamp stored in visit backend.")

                            } else {
                                Log.d("mytag", "unable to update fitbit timestamp")
                            }
                        }
                    }


                } else {
                    fitbitStepsResponse.errorMessage?.let {
                        Log.d("mytag", "errorMessage: $it")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun revokeFitbitAccess() {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val visitBaseUrl = sharedPrefUtil.getVisitBaseUrl()

                val visitAuthToken = sharedPrefUtil.getVisitAuthToken()

                val visitApiService = getVisitApiService(visitBaseUrl, visitAuthToken)

                val response = visitApiService.revokeFitBitAccess()
                Log.d("mytag", "revoke response: $response")
                if (response.message == "success") {
                    sharedPrefUtil.setFitBitConnectedStatus(false)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFitbitCurrentStatus(): Boolean {
        return sharedPrefUtil.getFitBitConnectionStatus()
    }


}