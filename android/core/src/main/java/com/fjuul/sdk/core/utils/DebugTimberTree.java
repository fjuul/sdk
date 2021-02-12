package com.fjuul.sdk.core.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.util.Log;
import timber.log.Timber;

/**
 * Debug implementation of {@link TimberTree} that writes messages with any priority via android's
 * {@link Log}.<br>
 * You can extend this class and override {@link #isLoggable(String, int)} to keep logs only with the desired priority.
 */
public class DebugTimberTree extends TimberTree {
    private final Timber.DebugTree delegate;
    private @Nullable Method delegateMethod;

    public DebugTimberTree() {
        this.delegate = new Timber.DebugTree();
        try {
            delegateMethod =
                delegate.getClass().getDeclaredMethod("log", int.class, String.class, String.class, Throwable.class);
            delegateMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            Log.e(DebugTimberTree.class.getSimpleName(), "Can't access the 'log' method of Timber.DebugTree");
            delegateMethod = null;
        }
    }


    @Override
    protected void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        try {
            delegateMethod.invoke(delegate, priority, tag, message, t);
        } catch (IllegalAccessException | InvocationTargetException e) {}
    }
}
