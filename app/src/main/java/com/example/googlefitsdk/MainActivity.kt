package com.example.googlefitsdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.googlefitsdk.databinding.MainActivityBinding
import com.getvisitapp.google_fit.GenericListener
import com.getvisitapp.google_fit.GoogleFitConnector
import com.getvisitapp.google_fit.StepsCounter
import com.getvisitapp.google_fit.pojo.HealthDataGraphValues
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class MainActivity : AppCompatActivity(), GenericListener {
    var TAG = this.javaClass.simpleName
    lateinit var binding: MainActivityBinding
    private var stepsCounter: StepsCounter? = null
    lateinit var googleFitConnector: GoogleFitConnector
    private lateinit var healthDataGraphValuesSubscriber: Subscriber<HealthDataGraphValues>
    var default_web_client_id =
        "74319562719-7rart63dq265045vtanlni9m8o41tn7o.apps.googleusercontent.com"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        healthDataGraphValuesSubscriber = object : Subscriber<HealthDataGraphValues>() {
            override fun onCompleted() {
                Log.d(TAG, "onCompleted: healthDataGraphValuesSubscriber");
            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

            override fun onNext(t: HealthDataGraphValues?) {
                Log.d(TAG, "healthDataValues $t")
                Log.d(TAG, "totalActivityTime: ${t?.totalActivityTimeInMinutes}")
                Log.d(TAG, "activityType: ${t?.activityType}")
                Log.d(TAG, "values: ${t?.values}")
                t?.activitySession?.forEach { activitySession ->
                    Log.d(
                        TAG,
                        "startTime: ${activitySession.sessionStart} endTime:${activitySession.sessionEnd}  totalActivityTime:${activitySession.totalActivityTime} value: ${activitySession.value}"
                    )
                }


            }
        }

        googleFitConnector = GoogleFitConnector(
            this,
            default_web_client_id,
            object : GoogleFitConnector.GoogleConnectorFitListener {
                override fun onComplete() {
                    Log.d(TAG, "onComplete() called")
                }

                override fun onError() {
                    Log.d(TAG, "onError() called")

                }

                override fun onServerAuthCodeFound(p0: String?) {
                    Log.d(TAG, "error Occured: $p0")
                }

            })

        binding.button.setOnClickListener {
            stepsCounter = StepsCounter.getInstance(this)

            stepsCounter!!.run(
                default_web_client_id,
                GenericListener {
                    Log.d(TAG, "Job Done: $it");

                    var calendar = Calendar.getInstance()
                    calendar.time = Date()
                    calendar.firstDayOfWeek = 2
                    calendar.set(2021, 9, 22, 10, 0, 0)
                    val startOfDay = calendar.timeInMillis;

                    calendar.set(2021, 9, 30, 10, 0, 0)
                    val endOfDay = calendar.timeInMillis



                    googleFitConnector.getWeeklySteps(startOfDay, endOfDay)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(healthDataGraphValuesSubscriber)


                })

        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (stepsCounter != null) {
            stepsCounter!!.onActivityResult(requestCode, resultCode, data, this)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onJobDone(email: String?) {
        stepsCounter!!.run() { // Do Something here
            Log.d("mytag", "Job Done setGoogleFitPermisson() called")

        }
    }
}