package com.fjuul.sdk.activitysources.entities.internal;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;

public class SupervisedTask<T> {
    private final @NonNull String name;
    private final @NonNull Task<T> task;
    private final long timeoutSeconds;
    private final int retriesCount;

    public SupervisedTask(@NonNull String name, @NonNull Task<T> task, long timeoutSeconds, int retriesCount) {
        this.name = name;
        this.task = task;
        this.timeoutSeconds = timeoutSeconds;
        this.retriesCount = retriesCount;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public Task<T> getTask() {
        return task;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getRetriesCount() {
        return retriesCount;
    }
}
