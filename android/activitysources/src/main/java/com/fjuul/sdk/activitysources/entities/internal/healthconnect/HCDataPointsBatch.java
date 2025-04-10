package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.List;

import androidx.annotation.NonNull;

public class HCDataPointsBatch {
    @NonNull
    private final List<? extends HCDataPoint> dataPoints;

    public HCDataPointsBatch(@NonNull List<? extends HCDataPoint> dataPoints) {
        this.dataPoints = dataPoints;
    }

    @NonNull
    public List<? extends HCDataPoint> getDataPoints() {
        return dataPoints;
    }
} 