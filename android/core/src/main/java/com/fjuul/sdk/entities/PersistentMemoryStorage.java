package com.fjuul.sdk.entities;

import android.content.SharedPreferences;

public class PersistentMemoryStorage implements IStorage {
    SharedPreferences preferences;

    public PersistentMemoryStorage(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public void set(String key, String value) {
        if (value == null) {
            preferences.edit().remove(key).commit();
        } else {
            preferences.edit().putString(key, value).commit();
        }
    }

    @Override
    public String get(String key) {
        return preferences.getString(key, null);
    }
}
