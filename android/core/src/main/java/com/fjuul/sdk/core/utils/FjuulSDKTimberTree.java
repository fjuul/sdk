package com.fjuul.sdk.core.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

public abstract class FjuulSDKTimberTree extends Timber.Tree {
    protected static final Pattern SDK_MODULE_NAME = Pattern.compile("^com.fjuul.sdk.([^.]+)\\.");
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    @Override protected boolean isLoggable(@Nullable String tag, int priority) {
        return FjuulSDKLogger.TAG.equals(tag);
    }

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        final List<StackTraceElement> filteredTrace = getFilteredStackTraceOfCaller();
        String messagePrefix = "";
        if (filteredTrace.size() > 0) {
            final String callerClassName = inferCallerClassName(filteredTrace.get(0));
            final String sdkModuleName = inferSdkModuleName(filteredTrace.get(0));
            final StringBuilder messagePrefixBuilder = new StringBuilder();
            if (sdkModuleName != null) {
                messagePrefixBuilder.append(String.format(Locale.ROOT, "[%s]", sdkModuleName));
            }
            messagePrefix = messagePrefixBuilder.append(String.format(Locale.ROOT," %s: ", callerClassName)).toString();
        }
        final String correctedMessage = messagePrefix + message;
        doLog(priority, tag, correctedMessage, t);
    }

    protected abstract void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t);

    protected List<StackTraceElement> getFilteredStackTraceOfCaller() {
        final StackTraceElement[] rawStackTrace = new Throwable().getStackTrace();
        final ArrayList<StackTraceElement> filteredStackTrace = new ArrayList<>();
        boolean isReachedTimberFromTop = false;
        boolean isReachedSdkAfterTimber = false;
        for (final StackTraceElement element : rawStackTrace) {
            if (!isReachedTimberFromTop) {
                if (isTimberStackTraceElement(element)) {
                    isReachedTimberFromTop = true;
                }
                continue;
            }
            if (!isReachedSdkAfterTimber) {
                if (isTimberStackTraceElement(element)) {
                    continue;
                }
                isReachedSdkAfterTimber = true;
            }
            filteredStackTrace.add(element);
        }
        return filteredStackTrace;
    }

    private boolean isTimberStackTraceElement(@NonNull StackTraceElement element) {
        String elementClassName = element.getClassName();
        Matcher matcher = ANONYMOUS_CLASS.matcher(elementClassName);
        if (matcher.find()) {
            elementClassName = matcher.replaceAll("");
        }
        return elementClassName.equals(Timber.class.getName());
    }

    protected String inferCallerClassName(@NonNull StackTraceElement element) {
        final String fullClassName = element.getClassName();
        return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
    }

    @Nullable
    protected String inferSdkModuleName(@NonNull StackTraceElement element) {
        final String fullClassName = element.getClassName();
        final Matcher matcher = SDK_MODULE_NAME.matcher(fullClassName);
        return matcher.find() ? matcher.group(1) : null;
    }
}
