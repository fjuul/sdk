package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class HCDataPoint {
    @NonNull
    private final Date start;
    @Nullable
    private final Date end;
    @NonNull
    private final Set<String> dataSources;

    public HCDataPoint(@NonNull Date start, @Nullable Date end) {
        this.start = start;
        this.end = end;
        this.dataSources = new HashSet<>();
    }

    @NonNull
    public Date getStart() {
        return start;
    }

    @Nullable
    public Date getEnd() {
        return end;
    }

    @NonNull
    public Set<String> getDataSources() {
        return dataSources;
    }

    public void addDataSource(@NonNull String source) {
        dataSources.add(source);
    }

    public void addDataSources(@NonNull Set<String> sources) {
        dataSources.addAll(sources);
    }

    public boolean isDaily() {
        return end == null;
    }
} 