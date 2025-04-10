package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCSessionBundle {
    @NonNull
    private final String activityType;
    @NonNull
    private final Date start;
    @NonNull
    private final Date end;
    @Nullable
    private final String dataSource;

    public HCSessionBundle(@NonNull String activityType, @NonNull Date start, @NonNull Date end,
        @Nullable String dataSource) {
        this.activityType = activityType;
        this.start = start;
        this.end = end;
        this.dataSource = dataSource;
    }

    @NonNull
    public String getActivityType() {
        return activityType;
    }

    @NonNull
    public Date getStart() {
        return start;
    }

    @NonNull
    public Date getEnd() {
        return end;
    }

    @Nullable
    public String getDataSource() {
        return dataSource;
    }
} 