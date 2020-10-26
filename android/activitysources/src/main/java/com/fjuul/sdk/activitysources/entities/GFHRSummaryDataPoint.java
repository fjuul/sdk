package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;

import java.util.Date;

public class GFHRSummaryDataPoint extends GFDataPoint {
    protected final float avg;
    protected final float min;
    protected final float max;

    public GFHRSummaryDataPoint(float avg, float min, float max, @NonNull Date start, @NonNull String dataSource) {
        super(start, dataSource);
        this.avg = avg;
        this.min = min;
        this.max = max;
    }
}
