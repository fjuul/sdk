package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class GFWeightDataPoint extends GFScalarDataPoint<Float> {
    public GFWeightDataPoint(@NonNull Float kg, @NonNull Date start) {
        super(kg, start, null);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale.getDefault(), "GFHeightDataPoint: kg %.1f, start %s", value, startFormatted);
    }
}
