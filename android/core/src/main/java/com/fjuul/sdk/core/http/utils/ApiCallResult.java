package com.fjuul.sdk.core.http.utils;

import com.fjuul.sdk.core.entities.AbstractResult;
import com.fjuul.sdk.core.exceptions.ApiExceptions.CommonException;

import androidx.annotation.NonNull;

public final class ApiCallResult<T> extends AbstractResult<T, CommonException> {
    @NonNull
    public static <T> ApiCallResult<T> value(@NonNull T value) {
        return new ApiCallResult<T>(value, null);
    }

    @NonNull
    public static <T> ApiCallResult<T> error(@NonNull CommonException error) {
        return new ApiCallResult<T>(null, error);
    }

    private ApiCallResult(T value, CommonException error) {
        super(value, error);
    }
}
