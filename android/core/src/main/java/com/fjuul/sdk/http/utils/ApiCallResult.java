package com.fjuul.sdk.http.utils;

import com.fjuul.sdk.errors.ApiErrors.CommonError;

public final class ApiCallResult<T> {
    public static <T> ApiCallResult<T> value(T value) {
        return new ApiCallResult<T>(value, null);
    }

    public static <T> ApiCallResult<T> error(CommonError error) {
        return new ApiCallResult<T>(null, error);
    }

    private final T value;
    private final CommonError error;

    private ApiCallResult(T value, CommonError error) {
        this.value = value;
        this.error = error;
    }

    public CommonError getError() {
        return error;
    }

    public T getValue() {
        return value;
    }

    public boolean isError() {
        return error != null;
    }
}
