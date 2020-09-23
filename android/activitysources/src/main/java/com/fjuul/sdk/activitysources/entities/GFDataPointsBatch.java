package com.fjuul.sdk.activitysources.entities;

import java.util.Date;
import java.util.List;

public class GFDataPointsBatch<T extends GFDataPoint> {
    private List<T> points;
    private Date startTime;
    private Date endTime;

    public List<T> getPoints() {
        return points;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public GFDataPointsBatch(List<T> points, Date startTime, Date endTime) {
        this.points = points;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
