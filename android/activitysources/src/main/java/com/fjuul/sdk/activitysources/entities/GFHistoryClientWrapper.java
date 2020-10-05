package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.core.util.Pair;

import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class GFHistoryClientWrapper {
    private static final String TAG = "GFHistoryClientWrapper";
    private static final int RETRIES_COUNT = 5;
    private static final int GF_TASK_WATCHER_THREAD_POOL_SIZE = 5;
    private static final long GF_QUERY_TIMEOUT_SECONDS = 60l;

    private HistoryClient client;
    private GFDataUtils gfUtils;
    private Executor executor;

    public GFHistoryClientWrapper(HistoryClient client, GFDataUtils gfUtils) {
        this.client = client;
        this.gfUtils = gfUtils;
        // TODO: choose the best executor for this wrapper needs (gf data converting)
        executor = Executors.newCachedThreadPool();
    }

    @SuppressLint("NewApi")
    public Task<List<GFCalorieDataPoint>> getCalories(Date start, Date end) {
        // NOTE: GF can silently fail on a request if response data is too large,
        // more details at https://stackoverflow.com/a/55806509/6685359
        // TODO: adjust size of chunks (duration) for the best performance
        ExecutorService gfTaskWatcherExecutor = Executors.newFixedThreadPool(GF_TASK_WATCHER_THREAD_POOL_SIZE);
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        List<Task<List<GFCalorieDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadCaloriesTask(dateRange, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        }).collect(Collectors.toList());

        Task<List<GFCalorieDataPoint>> getCaloriesTask = Tasks.whenAll(tasks).continueWithTask(executor, commonResult -> {
            if (commonResult.isCanceled()) {
                return Tasks.forException(new Exception("Pooling task was canceled"));
            } else if (commonResult.getException() != null) {
                Optional<Task<List<GFCalorieDataPoint>>> failedTaskOpt = tasks.stream()
                    .filter(t -> t.getException() != null)
                    .findFirst();
                Exception exception = failedTaskOpt.map(Task::getException).orElse(commonResult.getException());
                Tasks.forException(exception);
            }
            List<GFCalorieDataPoint> flattenList = tasks.stream()
                .flatMap(t -> t.getResult().stream())
                .collect(Collectors.toList());
            return Tasks.forResult(flattenList);
        })
            .addOnCompleteListener(executor, (task) -> {
//            Log.d(TAG, "getCalories: shutdown!");
            // TODO: check if it was really shutdown after a while
            gfTaskWatcherExecutor.shutdownNow();
        });
        return getCaloriesTask;
    }

    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Pair<Date, Date> dateRange, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        TaskCompletionSource<List<GFCalorieDataPoint>> taskCompletionSource = new TaskCompletionSource<>(cancellationToken);
        gfTaskWatcherExecutor.execute(() -> {
            if (cancellationToken.isCancellationRequested()) {
                return;
            }
            for (int tryNumber = 1; tryNumber <= RETRIES_COUNT && !cancellationToken.isCancellationRequested(); tryNumber++) {
//                Log.d(TAG, String.format("runReadCaloriesTask: awaiting #%d for %s", tryNumber, dateRange.toString()));
                try {
                    Task<List<GFCalorieDataPoint>> originalTask = readCaloriesHistory(dateRange.first, dateRange.second)
                        .onSuccessTask(executor, this::convertToCalories);
                    List<GFCalorieDataPoint> result = Tasks.await(originalTask, GF_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
//                    Log.d(TAG, String.format("runReadCaloriesTask: completed #%d for %s (size: %d)", tryNumber, dateRange.toString(),result.size()));
                    taskCompletionSource.trySetResult(result);
                    return;
                } catch (
                    // TODO: catch unauthorized exception
                    java.util.concurrent.ExecutionException | InterruptedException | java.util.concurrent.TimeoutException exc) {
//                    Log.d(TAG, String.format("runReadCaloriesTask: failed #%d for %s", tryNumber, dateRange.toString()));
                    continue;
                }
            }
            if (cancellationToken.isCancellationRequested()) {
                taskCompletionSource.trySetException(new Exception("One of the previous tasks was canceled"));
            } else {
                Log.d(TAG, "runReadCaloriesTask: Too many retries");
                // TODO: introduce new error class for the max retries exceeded case
                taskCompletionSource.trySetException(new Exception("Too many retries for date range " + dateRange));
                cancellationTokenSource.cancel();
            }
        });
        return taskCompletionSource.getTask();
    }

    private Task<List<GFCalorieDataPoint>> convertToCalories(DataReadResponse dataReadResponse) {
        ArrayList<GFCalorieDataPoint> calorieDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            DataSet dataSet = bucket.getDataSets().get(0);
            DataPoint calorieDataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = calorieDataPoint.getOriginalDataSource().getStreamIdentifier();
            for (Field field : DataType.TYPE_CALORIES_EXPENDED.getFields()) {
                if (Field.FIELD_CALORIES.equals(field)) {
                    float kcals = calorieDataPoint.getValue(field).asFloat();
                    GFCalorieDataPoint calorie = new GFCalorieDataPoint(kcals, start, dataSourceId);
                    calorieDataPoints.add(calorie);
                }
            }
        }
        return Tasks.forResult(calorieDataPoints);
    }

    private Task<DataReadResponse> readCaloriesHistory(Date start, Date end) {
        DataReadRequest readRequest = buildCaloriesDataReadRequest(start, end);
        return client.readData(readRequest);
    }

    private DataReadRequest buildCaloriesDataReadRequest(Date start, Date end) {
        return new DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }
}
