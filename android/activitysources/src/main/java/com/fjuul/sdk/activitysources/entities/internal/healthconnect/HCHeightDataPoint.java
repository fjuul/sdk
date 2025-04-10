package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCHeightDataPoint extends HCDataPoint {
    private final float height;

    public HCHeightDataPoint(float height, @NonNull Date timestamp) {
        super(timestamp, null);
        this.height = height;
    }

    public float getHeight() {
        return height;
    }
} 