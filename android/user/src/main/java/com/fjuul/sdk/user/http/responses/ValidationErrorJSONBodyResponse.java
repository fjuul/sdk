package com.fjuul.sdk.user.http.responses;

import java.util.List;

import com.fjuul.sdk.core.http.responses.ErrorJSONBodyResponse;

import androidx.annotation.NonNull;

public class ValidationErrorJSONBodyResponse extends ErrorJSONBodyResponse {
    @NonNull
    private List<ValidationError> errors;

    @NonNull
    public List<ValidationError> getErrors() {
        return errors;
    }
}
