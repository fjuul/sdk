package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;

public class HCStepsDataPoint extends HCDataPoint {
    private final int steps;

    public HCStepsDataPoint(@NonNull Date date, int steps) {
        super(date, null); // Daily data point, no end time
        this.steps = steps;
    }

    public int getSteps() {
        return steps;
    }
} 