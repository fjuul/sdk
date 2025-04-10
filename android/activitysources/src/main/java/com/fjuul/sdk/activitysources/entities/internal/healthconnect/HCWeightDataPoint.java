package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCWeightDataPoint extends HCDataPoint {
    private final float weight;

    public HCWeightDataPoint(@NonNull Date start, float weight, @Nullable String dataSource) {
        super(start, null, dataSource);
        this.weight = weight;
    }

    public float getWeight() {
        return weight;
    }
} 