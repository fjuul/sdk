package com.fjuul.sdk.user.http.responses;

import java.util.List;

import com.fjuul.sdk.core.http.responses.ErrorJSONBodyResponse;

public class ValidationErrorJSONBodyResponse extends ErrorJSONBodyResponse {
    private List<ValidationError> errors;

    public List<ValidationError> getErrors() {
        return errors;
    }
}
