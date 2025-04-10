package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCStepsDataPoint extends HCDataPoint {
    private final int steps;

    public HCStepsDataPoint(@NonNull Date start, @NonNull Date end, int steps, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.steps = steps;
    }

    public int getSteps() {
        return steps;
    }
} 