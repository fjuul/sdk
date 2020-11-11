package com.fjuul.sdk.http.utils;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.fjuul.sdk.exceptions.ApiExceptions;
import com.fjuul.sdk.http.responses.ErrorJSONBodyResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import androidx.annotation.NonNull;
import retrofit2.Response;

public class DefaultApiResponseTransformer<T> implements IApiResponseTransformer<T> {
    @Override
    public @NonNull ApiCallResult transform(@NonNull Response response) {
        if (response.isSuccessful()) {
            return ApiCallResult.value(response.body());
        }
        ErrorJSONBodyResponse responseBody;
        Moshi moshi = new Moshi.Builder().build();
        try {
            JsonAdapter<ErrorJSONBodyResponse> jsonAdapter = moshi.adapter(ErrorJSONBodyResponse.class);
            responseBody = jsonAdapter.fromJson(response.errorBody().source());
        } catch (IOException exc) {
            responseBody = null;
        }

        String errorMessage =
            responseBody != null && responseBody.getMessage() != null ? responseBody.getMessage() : "Unknown Error";
        ApiExceptions.CommonException exception;
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            ApiExceptions.UnauthorizedException.ErrorCode code;
            try {
                code = ApiExceptions.UnauthorizedException.ErrorCode
                    .valueOf(response.headers().get("x-authentication-error"));
            } catch (IllegalArgumentException | NullPointerException exc) {
                code = null;
            }
            exception = new ApiExceptions.UnauthorizedException(errorMessage, code);
        } else if (response.code() == HttpURLConnection.HTTP_BAD_REQUEST) {
            exception = new ApiExceptions.BadRequestException(errorMessage);
        } else {
            exception = new ApiExceptions.CommonException(errorMessage);
        }
        // TODO: add additional checks (forbidden)
        return ApiCallResult.error(exception);
    }
}
