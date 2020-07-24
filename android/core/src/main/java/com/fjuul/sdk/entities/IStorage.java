package com.fjuul.sdk.entities;

public interface IStorage {
    void set(String key, String value);
    String get(String key);
}
