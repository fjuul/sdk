package com.fjuul.sdk.entities;

import java.util.Date;

public class SigningKey {
    private String id;
    private String secret;
    private Date expiresAt;

    private Boolean valid;

    public SigningKey(
            String id, String secret, Date expiresAt, Boolean valid) {
        this.id = id;
        this.secret = secret;
        this.expiresAt = expiresAt;
        this.valid = valid;
    }

    public SigningKey(String id, String secret, Date expiresAt, String identityType) {
        this(id, secret, expiresAt, true);
    }

    public String getId() {
        return id;
    }

    public String getSecret() {
        return secret;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }
}
