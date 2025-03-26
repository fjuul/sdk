package com.fjuul.sdk.activitysources.entities.internal.healthconnect.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class HCHeightDataPoint extends HCScalarDataPoint<Float> {
    public HCHeightDataPoint(@NonNull Float cm, @NonNull Date start, @NonNull String dataSource) {
        super(cm, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale
            .getDefault(), "HCHeightDataPoint: cm %.2f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
