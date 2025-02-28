package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;

public class HCStepsDataPoint extends HCScalarDataPoint<Integer> {
    public HCStepsDataPoint(@NonNull Integer integer,
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
        return String.format(Locale
            .getDefault(), "HCStepsDataPoint: steps %d, start %s, dataSource %s", value, startFormatted, dataSource);
    }
}
