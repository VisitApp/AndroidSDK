package com.getvisitapp.google_fit.data;

import android.content.Context;
import android.util.Log;

import com.getvisitapp.google_fit.GoogleFitConnector;
import com.getvisitapp.google_fit.okhttp.ApiResponse;
import com.getvisitapp.google_fit.okhttp.MainActivityPresenter;
import com.getvisitapp.google_fit.okhttp.Transformers;
import com.getvisitapp.google_fit.pojo.ActivitySummaryGoal;
import com.getvisitapp.google_fit.pojo.HealthDataGraphValues;
import com.getvisitapp.google_fit.pojo.StartEndDate;
import com.getvisitapp.google_fit.util.DateHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Emitter;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * the purpose of this class is to exact the fitness data from google fit and pass it back to the caller.
 */
public class FitnessDataHelper {
    String TAG = "SyncStepHelper";
    private GoogleFitConnector googleFitConnector;
    private CompositeSubscription compositeSubscription;
    private Subscriber<ActivitySummaryGoal> activitySummarySubscriber;
 
    private long startSyncTime;
    private long endSyncTime;

    private Context context;
    private SimpleDateFormat readableFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
    FitnessDataHelperListener listener;


    public FitnessDataHelper(GoogleFitConnector connector,FitnessDataHelperListener listener) {
        this.googleFitConnector = connector;
        this.compositeSubscription = new CompositeSubscription();
        this.listener=listener;
        this.context = context;
    }

    public void dailySync(long googleFitLastSync) {


        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date()); // compute start of the day for the timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // long startOfDay = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        final long endOfDay = cal.getTimeInMillis();


        long startTime = googleFitLastSync;
        Log.d(TAG, "GoogleFitLastSync: " + startTime);

        Calendar last30Days;
        last30Days = Calendar.getInstance();
        last30Days.setTime(new Date());
        last30Days.set(Calendar.HOUR_OF_DAY, 0);
        last30Days.set(Calendar.MINUTE, 0);
        last30Days.set(Calendar.SECOND, 0);
        last30Days.set(Calendar.MILLISECOND, 0);
        last30Days.add(Calendar.DATE, -15);


        if (startTime == 0) {

            startTime = last30Days.getTimeInMillis();
        } else {

            int noOfDays = DateHelper.getDifferenceBetweenTwoDays(startTime, last30Days.getTimeInMillis());

            if (noOfDays > 15) {
                startTime = last30Days.getTimeInMillis();
            } else {

                // If the user has not updated his steps count for the first challenge then manually update his steps
                // This will update the step count for his previous 10 days
                // First challenge went live on 15th, so assuming that the user updates the app on 25th also, it will update his step count
                Calendar calendar;
                calendar = Calendar.getInstance();
                calendar.setTimeInMillis(startTime);
                calendar.add(Calendar.DATE, 0);
                startTime = calendar.getTimeInMillis();

            }
        }

        getSyncData(startTime, endOfDay);
        Log.d(TAG, "Start Time: " + startTime);
        Log.d(TAG, "End Of day: " + endOfDay);

        startSyncTime = startTime;
        endSyncTime = endOfDay;


    }

    private void getSyncData(long start, long end) {
        Log.d(TAG, "startTime: " + start + ", endTime: " + end);
        Log.d(TAG, "getSyncData: ");
        activitySummarySubscriber = new Subscriber<ActivitySummaryGoal>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(ActivitySummaryGoal goals) {
                Log.d(TAG, "onNext: " + goals.toString());


                showActivitySummary(goals);


                onCompleted();
            }
        };


        if (googleFitConnector != null) {
            Observable.zip(
                    googleFitConnector.getWeeklySteps(start, end),
                    googleFitConnector.getWeeklyDistance(start, end),
                    googleFitConnector.getWeeklyCalories(start, end),
                    googleFitConnector.getSleepForWeek(start, GoogleFitConnector.getDifferenceBetweenTwoDays(start, end)),
                    new Func4<HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, ActivitySummaryGoal>() {
                        @Override
                        public ActivitySummaryGoal call(HealthDataGraphValues healthDataGraphValues, HealthDataGraphValues healthDataGraphValues2, HealthDataGraphValues healthDataGraphValues3, HealthDataGraphValues healthDataGraphValues4) {
                            ActivitySummaryGoal activitySummaryGoal = new ActivitySummaryGoal(healthDataGraphValues, healthDataGraphValues2, healthDataGraphValues3, healthDataGraphValues4);
                            return activitySummaryGoal;
                        }

                    }
            )
                    .flatMap(new Func1<ActivitySummaryGoal, Observable<ActivitySummaryGoal>>() {
                        @Override
                        public Observable<ActivitySummaryGoal> call(ActivitySummaryGoal activitySummaryGoal) {
                            return Observable.just(activitySummaryGoal);
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(activitySummarySubscriber);
            compositeSubscription.add(activitySummarySubscriber);
        }
    }

    private void showActivitySummary(ActivitySummaryGoal goals) {
        ArrayList<Integer> arrayListCalories = new ArrayList<>();
        if (goals.calorie.getValues() == null) {
            for (int i = 0; i < goals.steps.getValues().size(); i++)
                arrayListCalories.add(i);
            goals.calorie.setValues(arrayListCalories);
        }


        ArrayList<Integer> arrayListDistance = new ArrayList<>();
        if (goals.distance.getValues() == null) {
            for (int i = 0; i < goals.distance.getValues().size(); i++)
                arrayListDistance.add(i);
            goals.distance.setValues(arrayListDistance);
        }


        final JsonObject postBody = new JsonObject();

        JsonArray jsonArray = new JsonArray();
        List<Long> time = DateHelper.convertSessioninDays(startSyncTime, endSyncTime);

        for (int i = 0; i < time.size(); i++) {

            long totalValue = 0;
            for (int j = 0; j < goals.steps.getActivitySession().size(); j++) {

                if (DateHelper.compareTime(time.get(i), TimeUnit.SECONDS.toMillis(goals.steps.getActivitySession().get(j).getSessionStart()))) {

                    if (i + 1 < time.size()) {
                        if ((DateHelper.compareTime(TimeUnit.SECONDS.toMillis(goals.steps.getActivitySession().get(j).getSessionEnd()), time.get(i + 1))))
                            totalValue = totalValue + goals.steps.getActivitySession().get(j).getValue();
                    } else {
                        totalValue = totalValue + goals.steps.getActivitySession().get(j).getValue();
                    }
                }
            }

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("steps", goals.steps.getValues().get(i));
            jsonObject.addProperty("calorie", goals.calorie.getValues().get(i));
            jsonObject.addProperty("distance", goals.distance.getValues().get(i));
            jsonObject.addProperty("activity", totalValue);
            if (i < (time.size() - 1))
                jsonObject.addProperty("sleep", goals.sleep.getSleepCards().get(i).getStartSleepTime() + "-" + goals.sleep.getSleepCards().get(i).getEndSleepTime());
            jsonObject.addProperty("date", time.get(i));

            jsonArray.add(jsonObject);


        }

        postBody.add("fitnessData", jsonArray);
        Log.d(TAG, "data sync api called:" + String.valueOf(postBody));
        sendData1(postBody);

    }

    private void sendData1(JsonObject payload) {

        listener.setDailyFitnessDataJSON(payload.toString());
    }


    public void hourlySync(long startTimeStamp) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(startTimeStamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        startTimeStamp = calendar.getTimeInMillis();
        Log.d(TAG, "onRunJob: " + startTimeStamp);
        Log.d(TAG, "onRunJob: " + readableFormat.format(startTimeStamp));
        // If difference between the timestamp and today is over 30 days, then only fetch recent 30 days data from google
        // And mark the job done once the 30 days data is synced
        // Each day needs to be run sequentially

        int diffDays = GoogleFitConnector.getDifferenceBetweenTwoDays(startTimeStamp, System.currentTimeMillis()) + 1;
        Log.d(TAG, "onRunJob: diffDays: " + diffDays);

        long endTimeStamp = 0;
        if (diffDays > 30) {
            //if diffDays are 30 or more, then we will sync only the recent 30 days.

            Calendar startCalendar = Calendar.getInstance();
            startCalendar.setTimeInMillis(System.currentTimeMillis());

            //doing to remove the hours passed in the today's date.
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);

            startCalendar.add(Calendar.DATE, -30); //going back to 30 days.
            startTimeStamp = startCalendar.getTimeInMillis();

        }

        endTimeStamp = System.currentTimeMillis();

        Log.d(TAG, "startTimeStamp:" + readableFormat.format(startTimeStamp));
        Log.d(TAG, "endTimeStamp:" + readableFormat.format(endTimeStamp));



        syncDataForDay(startTimeStamp, endTimeStamp, context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<JSONObject>() {
                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "********onCompleted**********: syncDataForDay: ");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(JSONObject jsonObject) {
                        Log.d(TAG, "onNext: jsonObject: " + jsonObject);

                        listener.setHourlyFitnessDataJSON(jsonObject.toString());

                    }
                });

    }

    private Observable<JSONObject> syncDataForDay(long startime, long endTime, Context context) {

        // Make a list of observables with responses as true and false and run them serially
        // If any of the observables return false, then end the chain
        HashMap<Long, Long> days = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        ArrayList<StartEndDate> list = new ArrayList<>();

        int diffDays = GoogleFitConnector.getDifferenceBetweenTwoDays(startime, endTime) + 1;
        for (int i = 0; i < diffDays; i++) {
            calendar.setTimeInMillis(startime);
            calendar.add(Calendar.DATE, 1);
            long endDayTimeStamp = calendar.getTimeInMillis();
            days.put(startime, endDayTimeStamp);
            list.add(new StartEndDate(startime, endDayTimeStamp));
            startime = endDayTimeStamp;
        }

        return getPayloadForDay(list.get(0).getStartTime(),list.get(0).getEndTime(), context);


    }

    private Observable<JSONObject> getPayloadForDay(long start, long end, Context context) {

        Log.d(TAG, "Start: " + start + " End: " + end);
        Log.d(TAG, "getPayloadForDay: " + readableFormat.format(start) + " to " + readableFormat.format(end));

        return Observable.zip(
                googleFitConnector.getDailySteps(start, end),
                googleFitConnector.getDailyDistance(start, end),
                googleFitConnector.getDailyCalories(start, end),
                new Func3<HealthDataGraphValues, HealthDataGraphValues, HealthDataGraphValues, JSONObject>() {
                    @Override
                    public JSONObject call(HealthDataGraphValues steps, HealthDataGraphValues distance, HealthDataGraphValues calories) {
                        //Log.d(TAG, "getPayloadForDay: Response: " + readableFormat.format(start) + " to " + readableFormat.format(end));
                        //Log.d(TAG, "call: steps :" + steps.getValues());
                        //Log.d(TAG, "call: distance :" + distance.getValues());
                        //Log.d(TAG, "call: calories :" + calories.getValues());
                        JSONArray data = new JSONArray();
                        JSONObject payload = new JSONObject();
                        JSONObject jsonObject;
                        try {
                            for (int i = 0; i < steps.getValues().size(); i++) {
                                jsonObject = new JSONObject();
                                jsonObject.put("st", steps.getValues().get(i));
                                jsonObject.put("c", calories.getValues().get(i));
                                jsonObject.put("d", distance.getValues().get(i));
                                jsonObject.put("h", i);
                                jsonObject.put("s", steps.getAppContributedToGoogleFitValues().get(i));
                                data.put(jsonObject);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        try {
                            payload.put("data", data);
                            payload.put("dt", start);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        return payload;
                    }
                }

        )
                .concatMap(new Func1<JSONObject, Observable<JSONObject>>() {
                    @Override
                    public Observable<JSONObject> call(JSONObject jsonObject) {
                        return Observable.just(jsonObject);
                    }
                });
    }


}


