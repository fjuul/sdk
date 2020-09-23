package com.fjuul.sdk.activitysources.entities;

import java.util.Date;

public class GFDataPoint<TValue> {
    protected TValue value;

    protected Date start;

    protected String dataSource;

    public TValue getValue() {
        return value;
    }

    public Date getStart() {
        return start;
    }

    public String getDataSource() {
        return dataSource;
    }

    public GFDataPoint(TValue value, Date start, String dataSource) {
        this.value = value;
        this.start = start;
        this.dataSource = dataSource;
    }
}
