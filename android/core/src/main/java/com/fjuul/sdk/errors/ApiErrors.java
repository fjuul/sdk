package com.fjuul.sdk.errors;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApiErrors {
    public static class CommonError extends FjuulError {
        public CommonError(@NonNull String message) {
            super(message);
        }

        public CommonError(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }

        public CommonError(@NonNull Throwable cause) {
            super(cause);
        }
    }

    /**
     * The error will be instantiated when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    public static class InternalClientError extends CommonError {
        public InternalClientError(@NonNull IOException exception) {
            super(exception.getMessage(), exception.getCause());
        }

        public InternalClientError(@NonNull RuntimeException exception) {
            super(exception.getMessage(), exception.getCause());
        }

        public InternalClientError(@NonNull Throwable throwable) {
            super(throwable);
        }
    }

    public static class UnauthorizedError extends CommonError {
        public enum ErrorCode {
            invalid_key_id,
            expired_signing_key,
            mismatched_request_signature,
            bad_signature_header,
            wrong_credentials,
            clock_skew
        }

        private ErrorCode errorCode;

        public @Nullable ErrorCode getErrorCode() {
            return errorCode;
        }

        public UnauthorizedError(@NonNull String message, @Nullable ErrorCode errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}