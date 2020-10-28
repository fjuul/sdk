package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFActivitySegmentDataPoint extends GFScalarDataPoint<Integer> {
    public GFActivitySegmentDataPoint(@NonNull Integer integer, @NonNull Date start, @NonNull Date end, @NonNull String dataSource) {
        super(integer, start, end, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String startFormatted = myFormat.format(start);
        String endFormatted = myFormat.format(end);
        return String.format("GFActivitySegmentDataPoint: type %d, start %s, end %s, dataSource %s", value, startFormatted, endFormatted, dataSource);
    }
}
