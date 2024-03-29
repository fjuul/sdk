package com.fjuul.sdk.activitysources.entities.internal;

import java.util.concurrent.ExecutorService;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;

import androidx.annotation.NonNull;

class SupervisedExecutor {
    private ExecutorService executor;

    private CancellationTokenSource cancellationTokenSource;

    private CancellationToken cancellationToken;

    public SupervisedExecutor(@NonNull ExecutorService executor,
        @NonNull CancellationTokenSource cancellationTokenSource,
        @NonNull CancellationToken cancellationToken) {
        this.executor = executor;
        this.cancellationTokenSource = cancellationTokenSource;
        this.cancellationToken = cancellationToken;
    }

    public SupervisedExecutor(@NonNull ExecutorService executor,
        @NonNull CancellationTokenSource cancellationTokenSource) {
        this(executor, cancellationTokenSource, cancellationTokenSource.getToken());
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public CancellationTokenSource getCancellationTokenSource() {
        return cancellationTokenSource;
    }

    public CancellationToken getCancellationToken() {
        return cancellationToken;
    }
}
