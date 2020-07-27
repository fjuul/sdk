package com.fjuul.sdk.entities;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistentStorage implements IStorage {
    // TODO: consider moving this string to the resources
    static final String PREFERENCES_NAME = "com.fjuul.sdk.persistence";

    SharedPreferences preferences;

    public PersistentStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
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
