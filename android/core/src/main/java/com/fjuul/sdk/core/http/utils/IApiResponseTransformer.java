package com.fjuul.sdk.core.http.utils;

import androidx.annotation.NonNull;
import retrofit2.Response;

/**
 * An interface transforms an original retrofit response into ApiCallResult.
 *
 * Various services could require to handle an api response differently than usual. An implementation of this interface
 * solves this problem.
 *
 * @see DefaultApiResponseTransformer
 *
 * @param <T> Type parameter of casted response type by retrofit converters
 */
public interface IApiResponseTransformer<T> {
    public @NonNull ApiCallResult<T> transform(@NonNull Response<T> response);
}
