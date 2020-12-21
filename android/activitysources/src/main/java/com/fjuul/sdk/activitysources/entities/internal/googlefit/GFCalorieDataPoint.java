package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFCalorieDataPoint extends GFScalarDataPoint<Float> {
    public GFCalorieDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        this(value, start, null, dataSource);
    }

    public GFCalorieDataPoint(@NonNull Float aFloat, @NonNull Date start, @NonNull Date end, @NonNull String dataSource) {
        super(aFloat, start, end, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startFormatted = myFormat.format(start);
        return String.format("GFCalorieDataPoint: kcals %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
