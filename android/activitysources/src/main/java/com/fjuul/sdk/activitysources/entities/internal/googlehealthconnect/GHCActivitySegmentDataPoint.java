package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GHCActivitySegmentDataPoint extends GHCScalarDataPoint<Integer> {
    public GHCActivitySegmentDataPoint(@NonNull Integer segmentType,
        @NonNull Date start,
        @NonNull Date end) {
        super(segmentType, start, end, null);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        final String endFormatted = dateFormat.format(end);
        return String.format(Locale.getDefault(),
            "GHCActivitySegmentDataPoint: type %d, start %s, end %s",
            value,
            startFormatted,
            endFormatted);
    }
}
