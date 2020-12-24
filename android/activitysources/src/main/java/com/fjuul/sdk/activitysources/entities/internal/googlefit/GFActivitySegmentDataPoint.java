package com.fjuul.sdk.activitysources.entities.internal.googlefit;

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
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        final String startFormatted = dateFormat.format(start);
        final String endFormatted = dateFormat.format(end);
        return String.format("GFActivitySegmentDataPoint: type %d, start %s, end %s, dataSource %s", value, startFormatted, endFormatted, dataSource);
    }
}
