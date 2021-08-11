package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class AggregatedDailyStats {

    float activeKcal;
    float bmr;

    @NonNull
    ActivityMeasure low;
    @NonNull
    ActivityMeasure moderate;
    @NonNull
    ActivityMeasure high;

    @NonNull
    public float getActiveKcal() {
        return activeKcal;
    }

    @NonNull
    public float getBmr() {
        return bmr;
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
