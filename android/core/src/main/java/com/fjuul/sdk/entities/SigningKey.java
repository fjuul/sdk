package com.fjuul.sdk.entities;

import java.util.Date;

import androidx.annotation.NonNull;

public class SigningKey {
    private String id;
    private String secret;
    private Date expiresAt;

    public SigningKey(@NonNull String id, @NonNull String secret, @NonNull Date expiresAt) {
        this.id = id;
        this.secret = secret;
        this.expiresAt = expiresAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getSecret() {
        return secret;
    }

    @NonNull
    public Date getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.before(new Date());
    }
}
