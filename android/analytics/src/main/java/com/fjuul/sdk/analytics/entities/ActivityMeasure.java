package com.fjuul.sdk.analytics.entities;

public class ActivityMeasure {
    double seconds;
    float metMinutes;

    ActivityMeasure(long seconds, int metMinutes) {
        this.seconds = seconds;
        this.metMinutes = metMinutes;
    }

    public double getSeconds() {
        return seconds;
    }

    public float getMetMinutes() {
        return metMinutes;
    }
}
