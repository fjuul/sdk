package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCRestingHeartRateDataPoint extends HCHeartRateDataPoint {
    public HCRestingHeartRateDataPoint(@NonNull Date timestamp, float heartRate, @Nullable String dataSource) {
        super(timestamp, null, heartRate, dataSource);
    }
} 