package com.fjuul.sdk.entities;

public interface IStorage {
    // TODO: add method to clear storage

    void set(String key, String value);

    String get(String key);
}
