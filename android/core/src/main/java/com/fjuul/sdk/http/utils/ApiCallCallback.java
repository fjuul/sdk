package com.fjuul.sdk.http.utils;

public interface ApiCallCallback<T> {
    public void onResponse(ApiCall<T> call, T responseBody);

    public void onFailure(ApiCall<T> call, Throwable t);
}
