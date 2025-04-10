package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCCaloriesDataPoint extends HCDataPoint {
    private final float totalCalories;
    private final float activeCalories;

    public HCCaloriesDataPoint(@NonNull Date start, @NonNull Date end, float totalCalories, float activeCalories, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.totalCalories = totalCalories;
        this.activeCalories = activeCalories;
    }

    public float getTotalCalories() {
        return totalCalories;
    }

    public float getActiveCalories() {
        return activeCalories;
    }
} 