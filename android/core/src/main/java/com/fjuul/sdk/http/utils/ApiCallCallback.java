package com.fjuul.sdk.http.utils;

import retrofit2.Retrofit;

/**
 * Communicates responses from a server or offline requests.
 *
 * <p>
 * Callback method is executed using the {@link Retrofit} callback executor. When none is specified, the following
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
     * Invoked for a received result.
     */
    public void onResult(ApiCall<T> call, ApiCallResult<T> result);
}
