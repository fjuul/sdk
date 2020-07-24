package com.fjuul.sdk.entities;

import android.annotation.SuppressLint;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

public class SigningKeychain {
    IStorage storage;
    String lookupKey;
    JsonAdapter<SigningKey> keyJsonAdapter;

    public SigningKeychain(IStorage storage, String userToken) {
        this.storage = storage;
        this.lookupKey = String.format("signing-key.%s", userToken);
        keyJsonAdapter = new Moshi.Builder()
            .add(Date.class, new Rfc3339DateJsonAdapter())
            .build()
            .adapter(SigningKey.class)
            .nullSafe();
    }

    public void setKey(SigningKey key) {
        // TODO: check if the key is valid
        if (key == null) {
            storage.set(lookupKey, null);
            return;
        }
        storage.set(lookupKey, keyJsonAdapter.toJson(key));
    }

    @SuppressLint("NewApi")
    public Optional<SigningKey> getValidKey() {
        String rawKeyPresentation = storage.get(lookupKey);
        if (rawKeyPresentation == null) {
            return Optional.empty();
        }
        try {
            SigningKey key = keyJsonAdapter.fromJson(storage.get(lookupKey));
            if (key != null && key.isExpired()) {
                return Optional.empty();
            }
            return Optional.ofNullable(key);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
