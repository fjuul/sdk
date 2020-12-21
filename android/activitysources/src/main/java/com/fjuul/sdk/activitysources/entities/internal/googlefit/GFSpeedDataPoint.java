package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFSpeedDataPoint extends GFScalarDataPoint<Float> {
    public GFSpeedDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startFormatted = myFormat.format(start);
        return String.format("GFSpeedDataPoint: m/s %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
