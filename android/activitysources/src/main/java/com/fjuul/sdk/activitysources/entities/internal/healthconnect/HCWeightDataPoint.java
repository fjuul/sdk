package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCWeightDataPoint extends HCDataPoint {
    private final float weight;

    public HCWeightDataPoint(float weight, @NonNull Date timestamp) {
        super(timestamp, null);
        this.weight = weight;
    }

    public float getWeight() {
        return weight;
    }
} 