package com.fjuul.sdk.core.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PersistentStorage implements IStorage {
    static final String BASE_PREFERENCES_NAME = "com.fjuul.sdk.persistence";

    @Nullable
    private volatile SharedPreferences preferences;
    @NonNull
    private final Context context;
    @NonNull
    private final String userToken;

    public PersistentStorage(@NonNull Context context, @NonNull String userToken) {
        this.context = context;
        this.userToken = userToken;
        this.preferences = context.getSharedPreferences(getSharedPrefsName(), Context.MODE_PRIVATE);
    }

    @Nullable
    private SharedPreferences getPreferences() {
        if (preferences == null) {
            throw new IllegalStateException("The storage was removed");
        }
        return preferences;
    }

    @Override
    public void set(@NonNull String key, @Nullable String value) {
        if (value == null) {
            getPreferences().edit().remove(key).commit();
        } else {
            getPreferences().edit().putString(key, value).commit();
        }
    }

    @Override
    @Nullable
    public String get(@NonNull String key) {
        return getPreferences().getString(key, null);
    }

    @Override
    public synchronized boolean remove() {
        final boolean editorResult = getPreferences().edit().clear().commit();
        boolean deleteFileResult = context.deleteSharedPreferences(getSharedPrefsName());
        preferences = null;
        return editorResult && deleteFileResult;
    }

    @SuppressLint("NewApi")
    private String getSharedPrefsName() {
        return String.join(".", BASE_PREFERENCES_NAME, userToken);
    }
}
