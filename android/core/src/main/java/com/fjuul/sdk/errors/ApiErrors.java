package com.fjuul.sdk.errors;

import java.io.IOException;

public final class ApiErrors {
    public static class CommonError extends FjuulError {
        public CommonError(String message) {
            super(message);
        }

        public CommonError(String message, Throwable cause) {
            super(message, cause);
        }

        public CommonError(Throwable cause) {
            super(cause);
        }
    }

    /**
     * The error will be instantiated when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    public static class InternalClientError extends CommonError {
        public InternalClientError(IOException exception) {
            super(exception.getMessage(), exception.getCause());
        }

        public InternalClientError(RuntimeException exception) {
            super(exception.getMessage(), exception.getCause());
        }

        public InternalClientError(Throwable throwable) {
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

        public ErrorCode getErrorCode() {
            return errorCode;
        }

        public UnauthorizedError(String message, ErrorCode errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }
}
