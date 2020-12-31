package com.fjuul.sdk.core.entities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

public class PersistentStorage implements IStorage {
    static final String BASE_PREFERENCES_NAME = "com.fjuul.sdk.persistence";

    @Nullable
    private volatile SharedPreferences preferences;
    @NonNull
    private Context context;
    @NonNull
    private String userToken;

    public PersistentStorage(@NonNull Context context, @NonNull String userToken) {
        this.context = context;
        this.preferences = context.getSharedPreferences(BASE_PREFERENCES_NAME, Context.MODE_PRIVATE);
        this.userToken = userToken;
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
        boolean deleteFileResult = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            deleteFileResult = context.deleteSharedPreferences(getSharedPrefsName());
        } else {
            final File sharedPrefsDir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
            final File[] sharedPrefsFiles = sharedPrefsDir.listFiles((dir, name) -> name.startsWith(getSharedPrefsName()));
            if (sharedPrefsFiles.length > 0) {
                final File sharedPrefsFile = sharedPrefsFiles[0];
                deleteFileResult = sharedPrefsFile.delete();
            }
        }
        preferences = null;
        return editorResult && deleteFileResult;
    }

    @SuppressLint("NewApi")
    private String getSharedPrefsName() {
        return String.join(".", BASE_PREFERENCES_NAME, userToken);
    }
}
