package com.fjuul.sdk.core.entities;

import androidx.annotation.Nullable;

public abstract class AbstractResult<T, E> {
    private final T value;
    private final E error;

    protected AbstractResult(@Nullable T value, @Nullable E error) {
        this.value = value;
        this.error = error;
    }

    @Nullable
    public E getError() {
        return error;
    }

    @Nullable
    public T getValue() {
        return value;
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isValue() {
        return value != null;
    }
}
