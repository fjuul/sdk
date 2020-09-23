package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public class GFCalorieDataPoint extends GFDataPoint<Float> {
    public GFCalorieDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("GFCalorieDataPoint: kcals %f, start %s, dataSource %s", value, start, dataSource);
    }
}
