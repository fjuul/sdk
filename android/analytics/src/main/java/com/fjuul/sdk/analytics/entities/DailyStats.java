package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class DailyStats {
    @NonNull
    String date;
    int steps;

    @NonNull
    ActivityMeasure low;
    @NonNull
    ActivityMeasure moderate;
    @NonNull
    ActivityMeasure high;

    @NonNull
    String[] contributingSources;

    @NonNull
    public String getDate() {
        return date;
    }

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

    @NonNull
    public String[] getContributingSources() {
        return contributingSources;
    }
}
