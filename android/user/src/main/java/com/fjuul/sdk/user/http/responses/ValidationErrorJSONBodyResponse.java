package com.fjuul.sdk.user.http.responses;

import com.fjuul.sdk.core.http.responses.ErrorJSONBodyResponse;

import java.util.List;

public class ValidationErrorJSONBodyResponse extends ErrorJSONBodyResponse {
    private List<ValidationError> errors;

    public List<ValidationError> getErrors() {
        return errors;
    }
}
