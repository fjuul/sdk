package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GFHeightDataPoint extends GFScalarDataPoint<Float> {
    public GFHeightDataPoint(@NonNull Float cm, @NonNull Date start) {
        super(cm, start, null);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale.getDefault(), "GFHeightDataPoint: cm %f, start %s, dataSource %s", value, startFormatted);
    }
}
