package com.fjuul.sdk.core.entities;

import android.util.Base64;
import androidx.annotation.NonNull;

// NOTE: this class was copied from the previous sdk
public class UserCredentials {
    private String token;
    private String secret;

    private UserCredentials() {
        // required by Retrofit
    }

    public UserCredentials(@NonNull String token, @NonNull String secret) {
        this.token = token;
        this.secret = secret;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public String getSecret() {
        return secret;
    }

    /**
     * Returns a Bearer token using the token and secret from this instance.
     *
     * @return a Bearer token using the token and secret from this instance.
     */
    @NonNull
    public String getCompleteAuthString() {
        return "Bearer " + this.encodedBase64();
    }

    /**
     * returns the base64 encoded credentials
     *
     * @return the base64 encoded credentials
     */
    @NonNull
    public String encodedBase64() {
        String together = token + ":" + secret;
        return Base64.encodeToString(together.getBytes(), Base64.NO_WRAP);
    }

    @Override
    @NonNull
    public String toString() {
        return "{ token: " + token + ", secret: *** }";
    }
}
