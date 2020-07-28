package com.fjuul.sdk.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IStorage {
    // TODO: add method to clear storage

    void set(@NonNull String key, @Nullable String value);

    @Nullable
    String get(@NonNull String key);
}
