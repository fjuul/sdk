package com.fjuul.sdk.core.utils;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import timber.log.Timber;

public class DebugFjuulSDKTimberTree extends FjuulSDKTimberTree {
    private final Timber.DebugTree delegate;
    private @Nullable Method delegateMethod;

    public DebugFjuulSDKTimberTree() {
        this.delegate = new Timber.DebugTree();
        try {
            delegateMethod = delegate.getClass().getDeclaredMethod("log", int.class, String.class, String.class, Throwable.class);
            delegateMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            Log.e(DebugFjuulSDKTimberTree.class.getSimpleName(), "Can't access the 'log' method of Timber.DebugTree");
            delegateMethod = null;
        }
    }


    @Override
    protected void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        try {
            delegateMethod.invoke(delegate, priority, tag, message, t);
        } catch (IllegalAccessException | InvocationTargetException e) { }
    }
}
