package com.fjuul.sdk.analytics.entities;

import androidx.annotation.NonNull;

public class ActivityMeasure {
    double seconds;
    float metMinutes;

    @NonNull
    public double getSeconds() {
        return seconds;
    }

    @NonNull
    public float getMetMinutes() {
        return metMinutes;
    }
}
