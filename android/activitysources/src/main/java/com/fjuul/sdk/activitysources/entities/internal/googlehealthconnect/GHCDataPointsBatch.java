package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;

public class GHCDataPointsBatch<T extends GHCDataPoint> {
    @NonNull
    private List<T> points;
    @NonNull
    private Date startTime;
    @NonNull
    private Date endTime;

    @NonNull
    public List<T> getPoints() {
        return points;
    }

    @NonNull
    public Date getStartTime() {
        return startTime;
    }

    @NonNull
    public Date getEndTime() {
        return endTime;
    }

    public GHCDataPointsBatch(@NonNull List<T> points, @NonNull Date startTime, @NonNull Date endTime) {
        this.points = points;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
