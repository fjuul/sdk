package com.fjuul.sdk.entities;

import java.util.HashMap;
import java.util.Map;

public class InMemoryStorage implements IStorage {
    private Map<String, String> store;

    public InMemoryStorage() {
        store = new HashMap<>();
    }

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }
}
