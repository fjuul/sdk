package com.fjuul.sdk.activitysources.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.annotation.SuppressLint;
import androidx.core.util.Supplier;

public final class GoogleTaskUtils {
    @SuppressLint("NewApi")
    public static <T extends Task<?>> Optional<Exception> extractGFExceptionFromTasks(List<T> tasks) {
        Optional<Exception> failedTaskOpt = tasks.stream()
            .filter(t -> t.getException() != null
                && t.getException() instanceof GoogleFitActivitySourceExceptions.CommonException)
            .map(Task::getException)
            .findFirst();
        return failedTaskOpt;
    }

    @SuppressLint("NewApi")
    public static <T> Task<T> shutdownExecutorsOnComplete(Executor shutdownExecutor,
        Task<T> task,
        ExecutorService... executors) {
        return task.addOnCompleteListener(shutdownExecutor, (t) -> {
            Arrays.stream(executors).forEach(executor -> executor.shutdownNow());
        });
    }

    public static <T> Task<T> runAndAwaitTaskByExecutor(Supplier<Task<T>> taskSupplier,
        Executor executor,
        CancellationTokenSource cancellationTokenSource,
        CancellationToken cancellationToken) {
        return Tasks.forResult(null).continueWithTask(executor, t -> {
            if (cancellationToken.isCancellationRequested()) {
                return Tasks.forCanceled();
            }
            try {
                T result = Tasks.await(taskSupplier.get());
                if (cancellationToken.isCancellationRequested()) {
                    return Tasks.forCanceled();
                }
                return Tasks.forResult(result);
            } catch (ExecutionException e) {
                cancellationTokenSource.cancel();
                if (e.getCause() instanceof Exception) {
                    return Tasks.forException((Exception) e.getCause());
                } else {
                    return Tasks.forException(e);
                }
            } catch (InterruptedException e) {
                return Tasks.forCanceled();
            }
        });
    }
}
