package com.fjuul.sdk.core.entities;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

public class Keystore {
    IStorage storage;
    String lookupKey;
    JsonAdapter<SigningKey> keyJsonAdapter;

    public Keystore(@NonNull IStorage storage) {
        this.storage = storage;
        this.lookupKey = "signing-key";
        keyJsonAdapter = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter())
            .build()
            .adapter(SigningKey.class)
            .nullSafe();
    }

    public void setKey(@NonNull SigningKey key) {
        // TODO: check if the key is valid
        if (key == null) {
            storage.set(lookupKey, null);
            return;
        }
        storage.set(lookupKey, keyJsonAdapter.toJson(key));
    }

    @SuppressLint("NewApi")
    @NonNull
    public Optional<SigningKey> getValidKey() {
        String rawKeyPresentation = storage.get(lookupKey);
        if (rawKeyPresentation == null) {
            return Optional.empty();
        }
        try {
            SigningKey key = keyJsonAdapter.fromJson(rawKeyPresentation);
            if (key != null && key.isExpired()) {
                return Optional.empty();
            }
            return Optional.ofNullable(key);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
