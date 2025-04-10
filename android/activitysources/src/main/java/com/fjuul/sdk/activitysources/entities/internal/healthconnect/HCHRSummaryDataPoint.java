package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCHRSummaryDataPoint extends HCDataPoint {
    private final float bpm;

    public HCHRSummaryDataPoint(float bpm, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.bpm = bpm;
    }

    public HCHRSummaryDataPoint(float bpm, @NonNull Date start, @NonNull Date end, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.bpm = bpm;
    }

    public float getBpm() {
        return bpm;
    }
} 