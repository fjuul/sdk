package com.fjuul.sdk.entities;

import androidx.annotation.NonNull;


public interface Callback<T> {
    public void onResult(@NonNull Result<T> result);
}
