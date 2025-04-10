package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCCalorieDataPoint extends HCDataPoint {
    private final float calories;

    public HCCalorieDataPoint(float calories, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.calories = calories;
    }

    public HCCalorieDataPoint(float calories, @NonNull Date start, @NonNull Date end, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.calories = calories;
    }

    public float getCalories() {
        return calories;
    }
} 