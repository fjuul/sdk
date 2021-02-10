package com.fjuul.sdk.core.utils;

import timber.log.Timber;

public final class FjuulSDKLogger {
    public static final String TAG = "Fjuul-SDK";

    public static Timber.Tree get() {
        return Timber.tag(TAG);
    }
}
