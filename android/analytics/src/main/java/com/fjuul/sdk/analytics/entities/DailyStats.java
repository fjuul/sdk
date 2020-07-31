package com.fjuul.sdk.analytics.entities;

public class DailyStats {
    String date;
    float activeKcal;
    float totalKcal;
    int steps;

    ActivityMeasure lowest;
    ActivityMeasure low;
    ActivityMeasure moderate;
    ActivityMeasure high;

    public String getDate() {
        return date;
    }

    public float getActiveKcal() {
        return activeKcal;
    }

    public float getTotalKcal() {
        return totalKcal;
    }

    public int getSteps() {
        return steps;
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
