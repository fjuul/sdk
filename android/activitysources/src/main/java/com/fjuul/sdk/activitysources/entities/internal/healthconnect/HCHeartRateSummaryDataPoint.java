package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCHeartRateSummaryDataPoint extends HCDataPoint {
    protected final float avg;
    protected final float min;
    protected final float max;

    public float getAvg() {
        return avg;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public HCHeartRateSummaryDataPoint(float avg,
                                       float min,
                                       float max,
                                       @NonNull Date start,
                                       @NonNull Date end,
                                       @Nullable String dataSource) {
        super(start, end, dataSource);
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    @NonNull
    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
        final String startFormatted = dateFormat.format(start);
        final String endFormatted = dateFormat.format(end);
        return String.format(Locale.getDefault(),
            "HCHeartRateSummaryDataPoint: avg BPM %f, min BPM %f, max BPM %f, start %s, end %s, dataSource %s",
            avg,
            min,
            max,
            startFormatted,
            endFormatted,
            dataSource);
    }
}
