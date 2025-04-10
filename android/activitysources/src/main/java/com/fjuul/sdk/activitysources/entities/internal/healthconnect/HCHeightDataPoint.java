package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCHeightDataPoint extends HCDataPoint {
    private final float height;

    public HCHeightDataPoint(@NonNull Date start, float height, @Nullable String dataSource) {
        super(start, null);
        this.height = height;
        if (dataSource != null) {
            addDataSource(dataSource);
        }
    }

    public float getHeight() {
        return height;
    }
} 