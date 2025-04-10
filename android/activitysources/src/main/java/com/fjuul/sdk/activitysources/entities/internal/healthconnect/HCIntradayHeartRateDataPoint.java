package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCIntradayHeartRateDataPoint extends HCHeartRateDataPoint {
    public HCIntradayHeartRateDataPoint(@NonNull Date start, @NonNull Date end, float heartRate, @Nullable String dataSource) {
        super(start, end, heartRate, dataSource);
    }
} 