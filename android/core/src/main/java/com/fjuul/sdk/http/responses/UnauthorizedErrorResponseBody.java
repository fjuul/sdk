package com.fjuul.sdk.http.responses;

public class UnauthorizedErrorResponseBody {
    // NOTE: keep synced with the back-end side
    public enum ErrorCode {
        invalid_key_id,
        expired_signing_key,
        mismatched_request_signature,
        bad_signature_header,
    }

    private String message;
    private ErrorCode errorCode;

    private UnauthorizedErrorResponseBody() { }

    public String getMessage() {
        return message;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
