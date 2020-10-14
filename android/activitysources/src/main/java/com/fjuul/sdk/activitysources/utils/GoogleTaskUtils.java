package com.fjuul.sdk.activitysources.utils;

import android.annotation.SuppressLint;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public final class GoogleTaskUtils {
    @SuppressLint("NewApi")
    public static <T extends Task<?>> Optional<Exception> extractGFExceptionFromTasks(List<T> tasks) {
        Optional<Exception> failedTaskOpt = tasks.stream()
            .filter(t -> t.getException() != null && t.getException() instanceof GoogleFitActivitySourceExceptions.CommonException)
            .map(Task::getException)
            .findFirst();
        return failedTaskOpt;
    }

    @SuppressLint("NewApi")
    public static <T> Task<T> shutdownExecutorsOnComplete(Executor shutdownExecutor, Task<T> task, ExecutorService... executors) {
        return task.addOnCompleteListener(shutdownExecutor, (t) -> {
            Arrays.stream(executors).forEach(executor -> executor.shutdownNow());
        });
    }


}
