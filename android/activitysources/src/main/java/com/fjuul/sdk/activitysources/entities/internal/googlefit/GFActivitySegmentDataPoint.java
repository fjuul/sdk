package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GFActivitySegmentDataPoint extends GFScalarDataPoint<Integer> {
    public GFActivitySegmentDataPoint(@NonNull Integer integer,
        @NonNull Date start,
        @NonNull Date end,
        @NonNull String dataSource) {
        super(integer, start, end, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        final String endFormatted = dateFormat.format(end);
        return String.format(Locale.getDefault(),
            "GFActivitySegmentDataPoint: type %d, start %s, end %s, dataSource %s",
            value,
            startFormatted,
            endFormatted,
            dataSource);
    }
}
