package com.fjuul.sdk.http.utils;

import retrofit2.Response;

public interface IApiResponseTransformer<T> {
    public ApiCallResult<T> transform(Response<T> response);
}
