package com.fjuul.sdk.entities;

import android.util.Base64;

// NOTE: this class was copied from the previous sdk
public class UserCredentials {
    private String token;
    private String secret;

    private UserCredentials() {
        // require by Retrofit
    }

    public UserCredentials(String token, String secret) {
        this.token = token;
        this.secret = secret;
    }

    public String getToken() {
        return token;
    }

    public String getSecret() {
        return secret;
    }

    /**
     * Returns a Bearer token using the token and secret from this instance.
     *
     * @return a Bearer token using the token and secret from this instance.
     */
    public String getCompleteAuthString() {
        return "Bearer " + this.encodedBase64();
    }

    /**
     * returns the base64 encoded credentials
     *
     * @return the base64 encoded credentials
     */
    public String encodedBase64() {
        String together = token + ":" + secret;
        return Base64.encodeToString(together.getBytes(), Base64.NO_WRAP);
    }

    @Override
    public String toString() {
        return "{ token: " + token + ", secret: *** }";
    }
}
