package com.fjuul.sdk.http.utils;


import androidx.annotation.NonNull;

/**
 * Communicates responses from a server or offline requests.
 *
 * <p>
 * Callback method is executed using the
 * <a href="https://square.github.io/retrofit/2.x/retrofit/retrofit2/Retrofit.html">Retrofit</a> callback executor. When
 * none is specified, the following defaults are used:
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
    public void onResult(@NonNull ApiCall<T> call, @NonNull ApiCallResult<T> result);
}
