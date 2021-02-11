package com.fjuul.sdk.test.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.fjuul.sdk.core.utils.FjuulSDKTimberTree;

import androidx.annotation.NonNull;

public class TestFjuulSDKTimberTree extends FjuulSDKTimberTree {
    @NonNull
    private List<TimberLogEntry> logEntries = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        logEntries.add(new TimberLogEntry(priority, message, t));
    }

    @NonNull
    public List<TimberLogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    public boolean isEmpty() {
        return logEntries.isEmpty();
    }

    @NonNull
    public TimberLogEntry removeFirst() {
        return logEntries.remove(0);
    }

    public int size() {
        return logEntries.size();
    }

    public void reset() {
        logEntries.clear();
    }
}
