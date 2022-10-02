package com.getvisitapp.google_fit.data;

import com.getvisitapp.google_fit.pojo.SleepCard;

public class StepsSleepCalorieData {
    public SleepCard sleepCard;
    public int steps;
    public float calorie;

    public StepsSleepCalorieData(SleepCard sleepCard, int steps, float calorie) {
        this.sleepCard = sleepCard;
        this.steps = steps;
        this.calorie = calorie;
    }
}
