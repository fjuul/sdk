package com.fjuul.sdk.exceptions;

import androidx.annotation.NonNull;

/**
 * The base exception class and all other exceptions in the sdk must be inherited from this.
 */
public class FjuulException extends Exception {
    public FjuulException(@NonNull String message) {
        super(message);
    }

    public FjuulException(@NonNull String message, @NonNull Throwable cause) {
        super(message, cause);
    }

    public FjuulException(@NonNull Throwable cause) {
        super(cause);
    }
}
