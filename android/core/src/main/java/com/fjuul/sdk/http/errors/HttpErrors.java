package com.fjuul.sdk.http.errors;

public final class HttpErrors {
    public static class CommonError extends Error {
        public CommonError(String message) {
            super(message);
        }

        public CommonError(String message, Throwable cause) {
            super(message, cause);
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
