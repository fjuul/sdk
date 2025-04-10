package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCStepsDataPoint extends HCDataPoint {
    private final int steps;

    public HCStepsDataPoint(int steps, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.steps = steps;
    }

    public HCStepsDataPoint(int steps, @NonNull Date start, @NonNull Date end, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.steps = steps;
    }

    public int getSteps() {
        return steps;
    }
} 