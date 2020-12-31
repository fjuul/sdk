package com.fjuul.sdk.core.entities;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class InMemoryStorage implements IStorage {
    @Nullable
    private volatile Map<String, String> store;

    public InMemoryStorage() {
        store = new HashMap<>();
    }

    @Nullable
    private Map<String, String> getStore() {
        if (store == null) {
            throw new IllegalStateException("The storage was removed");
        }
        return store;
    }

    @Override
    public void set(@NonNull String key, @Nullable String value) {
        getStore().put(key, value);
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        return getStore().get(key);
    }

    @Override
    public synchronized boolean remove() {
        getStore().clear();
        store = null;
        return true;
    }
}
