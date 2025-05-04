package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class AggregatedDailyStats {

    int steps;

    @NonNull
    ActivityMeasure low;
    @NonNull
    ActivityMeasure moderate;
    @NonNull
    ActivityMeasure high;

    public int getSteps() {
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
