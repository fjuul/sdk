package com.fjuul.sdk.http.utils;

import java.io.IOException;

import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class is almost re-implementation (most likely wrapper) of original retrofit's Call. It responds for:
 *
 * <ul>
 * <li>returning result without response/request information;
 * <li>handling error;
 * </ul>
 */
public class ApiCall<T> {
    private Call<T> delegate;
    private IApiResponseTransformer<T> responseTransformer;

    public ApiCall(Call<T> delegate, IApiResponseTransformer<T> responseTransformer) {
        this.delegate = delegate;
        this.responseTransformer = responseTransformer;
    }

    public ApiCallResult<T> execute() throws IOException {
        Response<T> response = delegate.execute();
        return responseTransformer.transform(response);
    }

    public void enqueue(ApiCallCallback<T, ApiCallResult<T>> callback) {
        delegate.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                callback.onResponse(new ApiCall(call, responseTransformer), responseTransformer.transform(response));
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                callback.onFailure(new ApiCall(call, responseTransformer), t);
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
        return new ApiCall(delegate.clone(), responseTransformer);
    }

    protected Request request() {
        return null;
    }

    public Timeout timeout() {
        return delegate.timeout();
    }
}
