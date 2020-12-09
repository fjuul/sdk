package com.fjuul.sdk.core.entities;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PersistentStorage implements IStorage {
    // TODO: consider moving this string to the resources
    static final String PREFERENCES_NAME = "com.fjuul.sdk.persistence";

    SharedPreferences preferences;

    public PersistentStorage(@NonNull Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void set(@NonNull String key, @Nullable String value) {
        if (value == null) {
            preferences.edit().remove(key).commit();
        } else {
            preferences.edit().putString(key, value).commit();
        }
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        return preferences.getString(key, null);
    }
}
