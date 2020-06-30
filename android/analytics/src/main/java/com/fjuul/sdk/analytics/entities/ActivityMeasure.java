package com.fjuul.sdk.analytics.entities;

public class ActivityMeasure {
    long seconds;
    int metMinutes;

    ActivityMeasure(long seconds, int metMinutes) {
        this.seconds = seconds;
        this.metMinutes = metMinutes;
    }

    public long getSeconds() {
        return seconds;
    }

    public int getMetMinutes() {
        return metMinutes;
    }
}
