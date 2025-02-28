package com.fjuul.sdk.activitysources.entities.internal.healthconnect;

import java.lang.reflect.ParameterizedType;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class HCScalarDataPoint<TValue extends Number> extends HCDataPoint {
    @NonNull
    protected final TValue value;

    @NonNull
    public TValue getValue() {
        return value;
    }

    public HCScalarDataPoint(@NonNull TValue value, @NonNull Date start, @Nullable String dataSource) {
        super(start, dataSource);
        this.value = value;
    }

    public HCScalarDataPoint(@NonNull TValue value,
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

        final HCScalarDataPoint<?> that = (HCScalarDataPoint<?>) o;

        Class<TValue> valueClass =
            (Class<TValue>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        Class<TValue> thatValueClass =
            (Class<TValue>) ((ParameterizedType) that.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        if (!valueClass.equals(thatValueClass)) {
            return false;
        }
        final HCScalarDataPoint<TValue> thatCasted = (HCScalarDataPoint<TValue>) o;

        if (valueClass.equals(Float.class)) {
            return Math.abs((Float) value - (Float) thatCasted.value) <= 0.0001;
        }
        return value.equals(that.value);
    }
}
