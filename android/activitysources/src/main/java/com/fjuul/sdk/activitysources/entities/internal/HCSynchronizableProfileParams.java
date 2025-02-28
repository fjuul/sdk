package com.fjuul.sdk.activitysources.entities.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HCSynchronizableProfileParams {
    @Nullable
    private Float height;
    @Nullable
    private Float weight;

    public void setHeight(@Nullable Float height) {
        this.height = height;
    }

    public void setWeight(@Nullable Float weight) {
        this.weight = weight;
    }

    public boolean isEmpty() {
        return height == null && weight == null;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("HCSynchronizableProfileParams{");
        boolean appended = false;
        if (height != null) {
            builder.append("height=\"").append(height).append("\"");
            appended = true;
        }
        if (weight != null) {
            if (appended) {
                builder.append(", ");
            }
            builder.append("weight=\"").append(weight).append("\"");
        }
        return builder.append("}").toString();
    }
}
