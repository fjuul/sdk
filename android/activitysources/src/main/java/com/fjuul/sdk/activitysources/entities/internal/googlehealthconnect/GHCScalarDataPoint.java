package com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect;

import java.lang.reflect.ParameterizedType;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class GHCScalarDataPoint<TValue extends Number> extends GHCDataPoint {
    @NonNull
    protected final TValue value;

    @NonNull
    public TValue getValue() {
        return value;
    }

    public GHCScalarDataPoint(@NonNull TValue value, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.value = value;
    }

    public GHCScalarDataPoint(@NonNull TValue value,
        @NonNull Date start,
        @Nullable Date end,
        @Nullable String dataSource) {
        super(start, end, dataSource);
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final GHCScalarDataPoint<?> that = (GHCScalarDataPoint<?>) o;

        Class<TValue> valueClass =
            (Class<TValue>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        Class<TValue> thatValueClass =
            (Class<TValue>) ((ParameterizedType) that.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (!valueClass.equals(thatValueClass)) {
            return false;
        }
        final GHCScalarDataPoint<TValue> thatCasted = (GHCScalarDataPoint<TValue>) o;

        if (valueClass.equals(Float.class)) {
            return Math.abs((Float) value - (Float) thatCasted.value) <= 0.0001;
        }
        return value.equals(that.value);
    }
}
