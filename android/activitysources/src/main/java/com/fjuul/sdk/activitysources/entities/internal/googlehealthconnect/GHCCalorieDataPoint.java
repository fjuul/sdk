package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GHCCalorieDataPoint extends GHCScalarDataPoint<Float> {
    public GHCCalorieDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        this(value, start, null, dataSource);
    }

    public GHCCalorieDataPoint(@NonNull Float aFloat,
        @NonNull Date start,
        @NonNull Date end,
        @NonNull String dataSource) {
        super(aFloat, start, end, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale
            .getDefault(), "GHCCalorieDataPoint: kcals %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}