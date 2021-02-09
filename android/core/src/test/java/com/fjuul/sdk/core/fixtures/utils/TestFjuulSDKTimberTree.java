package com.fjuul.sdk.core.fixtures.utils;

import com.fjuul.sdk.core.utils.FjuulSDKTimberTree;
import com.fjuul.sdk.core.fixtures.utils.TimberLogEntry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestFjuulSDKTimberTree extends FjuulSDKTimberTree {
    private List<TimberLogEntry> logEntries = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        logEntries.add(new TimberLogEntry(priority, message, t));
    }

    public List<TimberLogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    public void reset() {
        logEntries.clear();
    }
}
