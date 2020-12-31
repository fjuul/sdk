package com.fjuul.sdk.core.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IStorage {
    void set(@NonNull String key, @Nullable String value);

    @Nullable
    String get(@NonNull String key);

    void remove();
}
