package com.fjuul.sdk.http.utils;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class is almost re-implementation (most likely wrapper) of original retrofit's Call
 * It responds for:
 * - returning result without response/request information;
 * - handling error;
 */
public class ApiCall<T> {
    private Call<T> delegate;

    public ApiCall(Call<T> delegate) {
        this.delegate = delegate;
    }

    public Result<T, Error> execute() throws IOException {
        Response<T> response = delegate.execute();
        return convertResponseToResult(response);
    }

    public void enqueue(ApiCallCallback<T, Error, Result<T, Error>> callback) {
        delegate.enqueue(new Callback<T>() {
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

    protected Result<T, Error> convertResponseToResult(Response<T> response) {
        if (response.isSuccessful()) {
            return Result.value(response.body());
        }
        // TODO: add additional checks
        return Result.error(new Error("Unsuccessful request"));
    }
}
