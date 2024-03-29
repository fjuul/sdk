package com.fjuul.sdk.core.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import timber.log.Timber;

/**
 * A base class for all Timber Trees that log messages in FjuulSDK.<br>
 * The implementation overrides the required {@link Timber.Tree#log(int, String, String, Throwable)} method to
 * automatically infer the class and module names by the caller's stacktrace, but delegates writing the output itself to
 * sub-classes ({@link #doLog(int, String, String, Throwable)}).<br>
 * Usually, you may want to extend this class in the case of monitoring internal events of FjuulSDK by your event
 * tracker (Firebase Analytics, Sentry, etc.). Please check a set of overridable methods of {@link Timber.Tree} for
 * fully understanding capabilities.
 */
public abstract class TimberTree extends Timber.Tree {
    protected static final Pattern SDK_MODULE_NAME = Pattern.compile("^com\\.fjuul\\.sdk\\.([^.]+)\\.");

    @Override
    protected boolean isLoggable(@Nullable String tag, int priority) {
        return Logger.TAG.equals(tag);
    }

    @Override
    protected final void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        final List<StackTraceElement> filteredTrace = getFilteredStackTraceOfCaller();
        String messagePrefix = "";
        if (filteredTrace.size() > 0) {
            final String callerClassName = inferCallerClassName(filteredTrace.get(0));
            final String sdkModuleName = inferSdkModuleName(filteredTrace.get(0));
            final StringBuilder messagePrefixBuilder = new StringBuilder();
            if (sdkModuleName != null) {
                messagePrefixBuilder.append(String.format(Locale.ROOT, "[%s]", sdkModuleName));
            }
            messagePrefix =
                messagePrefixBuilder.append(String.format(Locale.ROOT, " %s: ", callerClassName)).toString();
        }
        final String correctedMessage = messagePrefix + message;
        doLog(priority, tag, correctedMessage, t);
    }

    protected abstract void doLog(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t);

    @NonNull
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
        return elementClassName.startsWith(Timber.class.getName());
    }

    @NonNull
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
