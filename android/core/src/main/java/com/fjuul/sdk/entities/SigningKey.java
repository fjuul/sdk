package com.fjuul.sdk.entities;

import java.util.Date;

public class SigningKey {
    private String id;
    private String secret;
    private Date expiresAt;
    // TODO: add enum for identityType
    private String identityType;

    private Boolean valid;

    public SigningKey(String id, String secret, Date expiresAt, String identityType, Boolean valid) {
        this.id = id;
        this.secret = secret;
        this.expiresAt = expiresAt;
        this.identityType = identityType;
        this.valid = valid;
    }

    public SigningKey(String id, String secret, Date expiresAt, String identityType) {
        this(id, secret, expiresAt, identityType, true);
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

    public String getIdentityType() {
        return identityType;
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }
}
