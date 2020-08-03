package com.fjuul.sdk.errors;

import androidx.annotation.NonNull;

/**
 * The basic class error and all other errors in the sdk must be inherited from this.
 */
public class FjuulError extends Error {
    public FjuulError(@NonNull String message) {
        super(message);
    }

    public FjuulError(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    public FjuulError(@NonNull Throwable cause) {
        super(cause);
    }
}
