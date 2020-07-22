package com.fjuul.sdk.http.utils;

import retrofit2.Retrofit;

/**
 * Communicates responses from a server or offline requests. One and only one method will be invoked in response to a
 * given request.
 *
 * <p>
 * Callback methods are executed using the {@link Retrofit} callback executor. When none is specified, the following
 * defaults are used:
 *
 * <ul>
 * <li>Android: Callbacks are executed on the application's main (UI) thread.
 * <li>JVM: Callbacks are executed on the background thread which performed the request.
 * </ul>
 *
 * @param <T> Successful response body type.
 */
public interface ApiCallCallback<T> {
    /**
     * Invoked for a received response.
     */
    public void onResponse(ApiCall<T> call, ApiCallResult<T> result);

    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected exception occurred creating
     * the request or processing the response.
     */
    public void onFailure(ApiCall<T> call, Throwable t);
}
