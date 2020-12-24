package com.fjuul.sdk.activitysources.entities.internal.googlefit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class GFHRSummaryDataPoint extends GFDataPoint {
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

    public GFHRSummaryDataPoint(float avg, float min, float max, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    @Override
    public String toString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        final String startFormatted = dateFormat.format(start);
        return String.format("GFHRSummaryDataPoint: avg BPM %f, min BPM %f, max BPM %f, start %s, dataSource %s", avg, min, max, startFormatted, dataSource);
    }
}
