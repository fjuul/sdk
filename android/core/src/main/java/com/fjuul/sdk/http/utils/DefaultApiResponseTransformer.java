package com.fjuul.sdk.http.utils;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.fjuul.sdk.errors.ApiErrors;
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
        ApiErrors.CommonError error;
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            ApiErrors.UnauthorizedError.ErrorCode code;
            try {
                code = ApiErrors.UnauthorizedError.ErrorCode.valueOf(response.headers().get("x-authentication-error"));
            } catch (IllegalArgumentException | NullPointerException exc) {
                code = null;
            }
            error = new ApiErrors.UnauthorizedError(errorMessage, code);
        } else {
            error = new ApiErrors.CommonError(errorMessage);
        }
        // TODO: add additional checks (bad_request, forbidden)
        return ApiCallResult.error(error);
    }
}
