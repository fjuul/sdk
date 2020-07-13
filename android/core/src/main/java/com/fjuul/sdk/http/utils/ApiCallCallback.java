package com.fjuul.sdk.http.utils;

public interface ApiCallCallback<T, E extends Error, R extends Result<T, E>> {
    public void onResponse(ApiCall<T> call, R result);

    public void onFailure(ApiCall<T> call, Throwable t);
}
