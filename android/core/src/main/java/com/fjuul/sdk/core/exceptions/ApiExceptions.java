package com.fjuul.sdk.core.exceptions;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApiExceptions {
    public static class CommonException extends FjuulException {
        public CommonException(@NonNull String message) {
            super(message);
        }

        public CommonException(@NonNull String message, @NonNull Throwable cause) {
            super(message, cause);
        }

        public CommonException(@NonNull Throwable cause) {
            super(cause);
        }
    }

    /**
     * The exception will be instantiated when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    public static class InternalClientException extends CommonException {
        private @Nullable Exception originalException;

        public InternalClientException(@NonNull IOException exception) {
            super(exception.getMessage(), exception.getCause());
            originalException = exception;
        }

        public InternalClientException(@NonNull RuntimeException exception) {
            super(exception.getMessage(), exception.getCause());
            originalException = exception;
        }

        public InternalClientException(@NonNull Throwable throwable) {
            super(throwable);
        }

        public @Nullable Exception getOriginalException() {
            return originalException;
        }
    }

    public static class UnauthorizedException extends CommonException {
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

        public UnauthorizedException(@NonNull String message, @Nullable ErrorCode errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    public static class BadRequestException extends CommonException {
        public BadRequestException(@NonNull String message) {
            super(message);
        }
    }

    public static class ConflictException extends CommonException {
        public ConflictException(@NonNull String message) { super(message); }
    }
}
