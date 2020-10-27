package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public abstract class GFDataPoint {
    @NonNull protected final Date start;
    @Nullable protected final Date end;
    @NonNull protected final String dataSource;

    @NonNull
    public Date getStart() {
        return start;
    }

    @Nullable
    public Date getEnd() {
        return end;
    }

    @NonNull
    public String getDataSource() {
        return dataSource;
    }

    public GFDataPoint(@NonNull Date start, @NonNull String dataSource) {
        this(start, null, dataSource);
    }

    public GFDataPoint(@NonNull Date start, @Nullable Date end, @NonNull String dataSource) {
        this.start = start;
        this.end = end;
        this.dataSource = dataSource;
    }
}
