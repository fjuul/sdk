package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class DailyStats {
    String date;
    float activeKcal;
    float totalKcal;
    int steps;

    ActivityMeasure lowest;
    ActivityMeasure low;
    ActivityMeasure moderate;
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
    public float getTotalKcal() {
        return totalKcal;
    }

    @NonNull
    public int getSteps() {
        return steps;
    }

    @NonNull
    public ActivityMeasure getLowest() {
        return lowest;
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
