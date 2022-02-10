package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class DailyStats {
    @NonNull
    String date;
    float activeKcal;
    float bmr;
    int steps;

    @NonNull
    ActivityMeasure low;
    @NonNull
    ActivityMeasure moderate;
    @NonNull
    ActivityMeasure high;

    @NonNull
    public String getDate() {
        return date;
    }

    @NonNull
    public float getActiveKcal() {
        return activeKcal;
    }

    @NonNull
    public float getBmr() {
        return bmr;
    }

    @NonNull
    public float getSteps() {
        return steps;
    }

    @NonNull
    public ActivityMeasure getLow() {
        return low;
    }

    @NonNull
    public ActivityMeasure getModerate() {
        return moderate;
    }

    @NonNull
    public ActivityMeasure getHigh() {
        return high;
    }
}
