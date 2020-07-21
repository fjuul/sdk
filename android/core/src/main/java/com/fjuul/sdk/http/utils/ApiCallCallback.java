package com.fjuul.sdk.http.utils;

public interface ApiCallCallback<T, R extends ApiCallResult<T>> {
    public void onResponse(ApiCall<T> call, R result);

    public void onFailure(ApiCall<T> call, Throwable t);
}
