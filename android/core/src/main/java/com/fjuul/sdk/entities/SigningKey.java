package com.fjuul.sdk.entities;

import java.util.Date;

public class SigningKey {
    private String id;
    private String secret;
    private Date expiresAt;

    public SigningKey(String id, String secret, Date expiresAt) {
        this.id = id;
        this.secret = secret;
        this.expiresAt = expiresAt;
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

    public boolean isExpired() {
        return expiresAt.before(new Date());
    }
}
