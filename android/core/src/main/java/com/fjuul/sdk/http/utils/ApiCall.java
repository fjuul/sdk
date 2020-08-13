package com.fjuul.sdk.http.utils;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

import com.fjuul.sdk.errors.ApiErrors;

import androidx.annotation.NonNull;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This class is almost re-implementation (wrapper) of original retrofit's Call. It's responsible for making network
 * request and producing the api call result which is either the requested value of the specified type or error.
 */
public class ApiCall<T> {
    private Call<T> delegate;
    private IApiResponseTransformer<T> responseTransformer;

    /**
     * @param delegate instance of retrofit's call to be wrapped of.
     * @param responseTransformer transformer which decides how to build the result of api call by the response.
     */
    public ApiCall(@NonNull Call<T> delegate, @NonNull IApiResponseTransformer<T> responseTransformer) {
        this.delegate = delegate;
        this.responseTransformer = responseTransformer;
    }

    /**
     * Synchronously send the request and return its result.
     */
    public @NonNull ApiCallResult<T> execute() {
        try {
            Response<T> response = delegate.execute();
            return responseTransformer.transform(response);
        } catch (IOException exc) {
            return ApiCallResult.error(new ApiErrors.InternalClientError(exc));
        } catch (RuntimeException exc) {
            return ApiCallResult.error(new ApiErrors.InternalClientError(exc));
        }
    }

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error occurred talking to
     * the server, creating the request, or processing the response.
     */
    public void enqueue(@NonNull ApiCallCallback<T> callback) {
        delegate.enqueue(new Callback<T>() {
            // Since we use custom Call adapter, a retrofit executor doesn't run a callback in the
            // main thread as it stated in the docs.
            // see https://square.github.io/retrofit/2.x/retrofit/retrofit2/Retrofit.Builder.html#callbackExecutor-java.util.concurrent.Executor-
            Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                mainHandler.post(() -> {
                    callback.onResult(ApiCall.this, responseTransformer.transform(response));
                });
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                mainHandler.post(() -> {
                    callback.onResult(ApiCall.this,
                        ApiCallResult.error(new ApiErrors.InternalClientError(t)));
                });
            }
        });
    };

    /**
     * Returns true if this call has been either {@linkplain #execute() executed} or
     * {@linkplain #enqueue(ApiCallCallback) enqueued}. It is an error to execute or enqueue a call more than once.
     */
    public boolean isExecuted() {
        return delegate.isExecuted();
    }

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not yet been executed it
     * never will be.
     */
    public void cancel() {
        delegate.cancel();
    }

    /** True if {@link #cancel()} was called. */
    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    /**
     * Create a new, identical call to this one which can be enqueued or executed even if this call has already been.
     */
    public @NonNull ApiCall<T> clone() {
        return new ApiCall(delegate.clone(), responseTransformer);
    }

    protected @NonNull Request request() {
        return delegate.request();
    }

    /**
     * Returns a timeout that spans the entire call: resolving DNS, connecting, writing the request body, server
     * processing, and reading the response body. If the call requires redirects or retries all must complete within one
     * timeout period.
     */
    public @NonNull Timeout timeout() {
        return delegate.timeout();
    }
}
