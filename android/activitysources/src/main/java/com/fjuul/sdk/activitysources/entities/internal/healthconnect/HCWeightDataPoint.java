package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class HCWeightDataPoint extends HCScalarDataPoint<Float> {
    public HCWeightDataPoint(@NonNull Float kg, @NonNull Date start, @NonNull String dataSource) {
        super(kg, start, dataSource);
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        return String.format(Locale
            .getDefault(), "HCHeightDataPoint: kg %.1f, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
