package com.fjuul.sdk.http.utils;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.fjuul.sdk.http.errors.HttpErrors.CommonError;
import com.fjuul.sdk.http.errors.HttpErrors.UnauthorizedError;
import com.fjuul.sdk.http.responses.ErrorJSONBodyResponse;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class is almost re-implementation (most likely wrapper) of original retrofit's Call. It
 * responds for:
 *
 * <ul>
 *   <li>returning result without response/request information;
 *   <li>handling error;
 * </ul>
 */
public class ApiCall<T> {
    private Call<T> delegate;

    public ApiCall(Call<T> delegate) {
        this.delegate = delegate;
    }

    public Result<T, CommonError> execute() throws IOException {
        Response<T> response = delegate.execute();
        return convertResponseToResult(response);
    }

    public void enqueue(ApiCallCallback<T, CommonError, Result<T, CommonError>> callback) {
        delegate.enqueue(
                new Callback<T>() {
                    @Override
                    public void onResponse(Call<T> call, Response<T> response) {
                        callback.onResponse(new ApiCall(call), convertResponseToResult(response));
                    }

                    @Override
                    public void onFailure(Call<T> call, Throwable t) {
                        callback.onFailure(new ApiCall(call), t);
                    }
                });
    };

    public boolean isExecuted() {
        return delegate.isExecuted();
    }

    public void cancel() {
        delegate.cancel();
    }

    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    public ApiCall<T> clone() {
        return new ApiCall(delegate.clone());
    }

    protected Request request() {
        return null;
    }

    public Timeout timeout() {
        return delegate.timeout();
    }

    protected Result<T, CommonError> convertResponseToResult(Response<T> response) {
        if (response.isSuccessful()) {
            return Result.value(response.body());
        }
        ErrorJSONBodyResponse responseBody;
        Moshi moshi = new Moshi.Builder().build();
        try {
            JsonAdapter<ErrorJSONBodyResponse> jsonAdapter =
                    moshi.adapter(ErrorJSONBodyResponse.class);
            responseBody = jsonAdapter.fromJson(response.errorBody().source());
        } catch (IOException exc) {
            responseBody = null;
        }

        String errorMessage =
                responseBody != null && responseBody.getMessage() != null
                        ? responseBody.getMessage()
                        : "Unknown Error";
        CommonError error;
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            UnauthorizedError.ErrorCode code;
            try {
                code =
                        UnauthorizedError.ErrorCode.valueOf(
                                response.headers().get("x-authentication-error"));
            } catch (IllegalArgumentException exc) {
                code = null;
            }
            error = new UnauthorizedError(errorMessage, code);
        } else {
            error = new CommonError(errorMessage);
        }
        // TODO: add additional checks
        return Result.error(error);
    }
}
