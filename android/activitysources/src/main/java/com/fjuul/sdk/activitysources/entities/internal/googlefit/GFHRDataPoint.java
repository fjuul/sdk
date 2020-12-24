package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFHRDataPoint extends GFScalarDataPoint<Float> {
    public GFHRDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        final String startFormatted = dateFormat.format(start);
        return String.format("GFHRDataPoint: bpm %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
