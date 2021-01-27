package com.fjuul.sdk.user.http.responses;

import androidx.annotation.NonNull;

import java.util.Map;

public class ValidationError {
    private @NonNull String property;
    private @NonNull Object value;
    private @NonNull Map<String, String> constraints;

    /**
     * Returns the name of the property that failed validation.
     * @return property name
     */
    @NonNull
    public String getProperty() {
        return property;
    }

    /**
     * Returns the value of the property that was validated
     * @return property value
     */
    @NonNull
    public Object getValue() {
        return value;
    }

    /**
     * Returns a map whose key is the name of the violated constraint and value is a detailed message of this violation.
     * @return map of constraints for that property
     */
    @NonNull
    public Map<String, String> getConstraints() {
        return constraints;
    }
}
