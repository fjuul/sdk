package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class HCDataPoint {
    @NonNull
    protected final Date start;
    /**
     * Note: end could be null for data points which have instant measurement (for example, HR, weight, height, etc).
     */
    @Nullable
    protected final Date end;
    @Nullable
    protected final String dataSource;

    @NonNull
    public Date getStart() {
        return start;
    }

    /**
     * Note: end could be null for data points which have instant measurement (for example, HR, weight, height, etc).
     */
    @Nullable
    public Date getEnd() {
        return end;
    }

    @Nullable
    public String getDataSource() {
        return dataSource;
    }

    public HCDataPoint(@NonNull Date start, @Nullable String dataSource) {
        this(start, null, dataSource);
    }

    public HCDataPoint(@NonNull Date start, @Nullable Date end, @Nullable String dataSource) {
        this.start = start;
        this.end = end;
        this.dataSource = dataSource;
    }
} 