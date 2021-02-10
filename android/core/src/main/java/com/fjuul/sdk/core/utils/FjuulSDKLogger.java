package com.fjuul.sdk.core.utils;

import timber.log.Timber;

/**
 * An internal helper class for retrieving the Timber logger with the pre-initialized tag.<br>
 * SDK consumers should not use it in their own applications.
 */
public final class FjuulSDKLogger {
    /**
     * Constant TAG which all log entries must have when published from Fjuul SDK. It can be used to distinguish what a
     * log entry comes from.
     */
    public static final String TAG = "Fjuul-SDK";

    /**
     * Returns Timber with the set tag.
     * @return Timber tree
     */
    public static Timber.Tree get() {
        return Timber.tag(TAG);
    }
}
