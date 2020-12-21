package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFStepsDataPoint extends GFScalarDataPoint<Integer> {
    public GFStepsDataPoint(@NonNull Integer value, @NonNull Date start, @NonNull String dataSource) {
        this(value, start, null, dataSource);
    }

    public GFStepsDataPoint(@NonNull Integer integer, @NonNull Date start, @NonNull Date end, @NonNull String dataSource) {
        super(integer, start, end, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startFormatted = myFormat.format(start);
        return String.format("GFStepsDataPoint: steps %d, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
