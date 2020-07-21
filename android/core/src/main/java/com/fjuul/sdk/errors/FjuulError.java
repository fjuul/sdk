package com.fjuul.sdk.errors;

/**
 * The basic class error and all other errors in the sdk must be inherited from this.
 */
public class FjuulError extends Error {
    public FjuulError(String message) {
        super(message);
    }

    public FjuulError(String message, Throwable cause) {
        super(message, cause);
    }
}
