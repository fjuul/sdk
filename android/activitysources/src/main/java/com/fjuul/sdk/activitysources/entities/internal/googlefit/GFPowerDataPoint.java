package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;

public class GFPowerDataPoint extends GFScalarDataPoint<Float> {
    public GFPowerDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        final String startFormatted = dateFormat.format(start);
        return String.format("GFPowerDataPoint: watts %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
