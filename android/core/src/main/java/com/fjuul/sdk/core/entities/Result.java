package com.fjuul.sdk.core.entities;

import androidx.annotation.NonNull;

public final class Result<T> extends AbstractResult<T, Throwable> {
    @NonNull
    public static <T> Result<T> value(@NonNull T value) {
        return new Result<>(value, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull Throwable error) {
        return new Result<T>(null, error);
    }

    private Result(T value, Throwable error) {
        super(value, error);
    }
}

