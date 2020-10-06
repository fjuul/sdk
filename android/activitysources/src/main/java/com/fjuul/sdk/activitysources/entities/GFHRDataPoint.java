package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFHRDataPoint extends GFDataPoint<Float> {
    public GFHRDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startFormatted = myFormat.format(start);
        return String.format("GFHRDataPoint: bpm %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
