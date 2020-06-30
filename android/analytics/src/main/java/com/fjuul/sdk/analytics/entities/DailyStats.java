package com.fjuul.sdk.analytics.entities;

public class DailyStats {
    String date;
    int activeCalories;

    ActivityMeasure lowest;
    ActivityMeasure low;
    ActivityMeasure moderate;
    ActivityMeasure high;

    public String getDate() {
        return date;
    }

    public int getActiveCalories() {
        return activeCalories;
    }

    public ActivityMeasure getLowest() {
        return lowest;
    }

    public ActivityMeasure getLow() {
        return low;
    }

    public ActivityMeasure getModerate() {
        return moderate;
    }

    public ActivityMeasure getHigh() {
        return high;
    }
}
