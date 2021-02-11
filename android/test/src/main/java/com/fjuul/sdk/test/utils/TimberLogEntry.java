package com.fjuul.sdk.test.utils;

import org.jetbrains.annotations.Nullable;

public class TimberLogEntry {
    private final int priority;
    @Nullable
    private final String message;
    @Nullable
    private final Throwable throwable;

    public TimberLogEntry(int priority, @Nullable String message, @Nullable Throwable throwable) {
        this.priority = priority;
        this.message = message;
        this.throwable = throwable;
    }

    public int getPriority() {
        return priority;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
