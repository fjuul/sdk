package com.fjuul.sdk.user.entities;

import androidx.annotation.NonNull;

public class UserCreationResult {
    private UserProfile user;
    private String secret;

    @NonNull
    public UserProfile getUser() {
        return user;
    }

    @NonNull
    public String getSecret() {
        return secret;
    }
}
