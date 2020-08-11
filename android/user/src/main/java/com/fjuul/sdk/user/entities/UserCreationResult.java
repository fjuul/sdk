package com.fjuul.sdk.user.entities;

public class UserCreationResult {
    private UserProfile user;
    private String secret;

    public UserProfile getUser() {
        return user;
    }

    public String getSecret() {
        return secret;
    }
}
