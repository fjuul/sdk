package com.fjuul.sdk.entities;

// TODO: get back android's base64 module after turning this plugin to the android plugin
import java.util.Base64;

// NOTE: this class was copied from the previous sdk
public class UserCredentials {
    private String token;
    private String secret;

    private UserCredentials() {
        // require by Retrofit
    }

    UserCredentials(String token, String secret) {
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
     * @return a Bearer token using the token and secret from this instance.
     */
    public String getCompleteAuthString() {
        return "Bearer " + this.encodedBase64();
    }

    /**
     * returns the base64 encoded credentials
     * @return the base64 encoded credentials
     */
    public String encodedBase64() {
        String together = token + ":" + secret;
        return Base64.getEncoder().encodeToString(together.getBytes());
    }

    @Override
    public String toString() {
        return "{ token: " + token + ", secret: " + secret + " }";
    }
}
