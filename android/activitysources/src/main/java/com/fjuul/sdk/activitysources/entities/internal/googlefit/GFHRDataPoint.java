package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GFHRDataPoint extends GFScalarDataPoint<Float> {
    public GFHRDataPoint(@NonNull Float value, @NonNull Date start, @NonNull String dataSource) {
        super(value, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale
            .getDefault(), "GFHRDataPoint: bpm %f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
