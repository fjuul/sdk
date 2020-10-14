package com.fjuul.sdk.activitysources.utils;

import android.annotation.SuppressLint;

import androidx.core.util.Supplier;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

    public static <T> Task<T> runAndAwaitTaskByExecutor(Supplier<Task<T>> taskSupplier, Executor executor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(cancellationToken);
        executor.execute(() -> {
            if (cancellationToken.isCancellationRequested()) {
                return;
            }
            try {
                T result = Tasks.await(taskSupplier.get());
                taskCompletionSource.trySetResult(result);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    taskCompletionSource.trySetException((Exception)e.getCause());
                } else {
                    taskCompletionSource.trySetException(e);
                }
                cancellationTokenSource.cancel();
            } catch (InterruptedException e) { /* task was interrupted due to cancellation */ }
        });
        return taskCompletionSource.getTask();
    }

}
