package com.getvisitapp.google_fit.data

import android.content.Context
import android.util.Log
import com.getvisitapp.google_fit.util.GoogleFitAccessChecker
import com.getvisitapp.google_fit.util.GoogleFitConnector
import com.getvisitapp.google_fit.util.GoogleFitConnector.GoogleConnectorFitListener

class VisitStepSyncHelper(var context: Context, var default_web_client_id: String) {

    private var TAG = "mytag1"
    private var sharedPrefUtil = SharedPrefUtil(context)
    private lateinit var syncStepHelper: SyncStepHelper
    private lateinit var checker: GoogleFitAccessChecker

    fun syncSteps() {

        checker = GoogleFitAccessChecker(context)

        if (checker.checkGoogleFitAccess()) {

            val tataAIGLastSyncTimeStamp = sharedPrefUtil.getTataAIGLastSyncTimeStamp()
            val baseUrl = sharedPrefUtil.getVisitBaseUrl()
            val authToken = sharedPrefUtil.getVisitAuthToken()
            val tataAIG_base_url = sharedPrefUtil.getTATA_AIG_BaseURL()
            val tata_aig_authToken = sharedPrefUtil.getTataAIGAuthToken()
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
}