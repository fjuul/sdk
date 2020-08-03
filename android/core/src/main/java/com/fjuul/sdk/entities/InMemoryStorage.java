package com.fjuul.sdk.entities;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InMemoryStorage implements IStorage {
    private Map<String, String> store;

    public InMemoryStorage() {
        store = new HashMap<>();
    }

    @Override
    public void set(@NonNull String key, @Nullable String value) {
        store.put(key, value);
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        return store.get(key);
    }
}
