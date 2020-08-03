package com.fjuul.sdk.http.utils;

import com.fjuul.sdk.errors.ApiErrors.CommonError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApiCallResult<T> {
    @NonNull
    public static <T> ApiCallResult<T> value(@NonNull T value) {
        return new ApiCallResult<T>(value, null);
    }

    @NonNull
    public static <T> ApiCallResult<T> error(@NonNull CommonError error) {
        return new ApiCallResult<T>(null, error);
    }

    private final T value;
    private final CommonError error;

    private ApiCallResult(T value, CommonError error) {
        this.value = value;
        this.error = error;
    }

    @Nullable
    public CommonError getError() {
        return error;
    }

    @Nullable
    public T getValue() {
        return value;
    }

    public boolean isError() {
        return error != null;
    }
}
