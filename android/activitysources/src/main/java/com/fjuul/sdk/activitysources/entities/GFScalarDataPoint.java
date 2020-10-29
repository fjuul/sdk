package com.fjuul.sdk.activitysources.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public abstract class GFScalarDataPoint<TValue extends Number> extends GFDataPoint {
    @NonNull protected final TValue value;

    @NonNull public TValue getValue() {
        return value;
    }

    public GFScalarDataPoint(@NonNull TValue value, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.value = value;
    }

    public GFScalarDataPoint(@NonNull TValue value, @NonNull Date start, @Nullable Date end, @Nullable String dataSource) {
        super(start, end, dataSource);
        this.value = value;
    }
}
