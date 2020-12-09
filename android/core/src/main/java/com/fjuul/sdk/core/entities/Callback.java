package com.fjuul.sdk.core.entities;

import androidx.annotation.NonNull;


public interface Callback<T> {
    public void onResult(@NonNull Result<T> result);
}
