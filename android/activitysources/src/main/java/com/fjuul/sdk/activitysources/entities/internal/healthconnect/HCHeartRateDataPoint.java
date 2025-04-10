package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class HCHeartRateDataPoint extends HCDataPoint {
    private final float heartRate;

    protected HCHeartRateDataPoint(@NonNull Date start, @Nullable Date end, float heartRate, @Nullable String dataSource) {
        super(start, end);
        this.heartRate = heartRate;
        if (dataSource != null) {
            addDataSource(dataSource);
        }
    }

    public float getHeartRate() {
        return heartRate;
    }
} 