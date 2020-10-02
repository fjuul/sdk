package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import androidx.arch.core.util.Function;
import androidx.core.util.Pair;

import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class GFHistoryClientWrapper {
    private static final String TAG = "GFHistoryClientWrapper";

    private HistoryClient client;
    private GFDataUtils gfUtils;
    private Executor executor;

    public GFHistoryClientWrapper(HistoryClient client, GFDataUtils gfUtils) {
        this.client = client;
        this.gfUtils = gfUtils;
        // TODO: choose the best executor for this wrapper needs (gf data converting)
        // NOTE: the code was taken from https://developers.google.com/android/guides/tasks#threading

//        this.executor = Executors.newCachedThreadPool();
//    newFixedThreadPool(50);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.executor = Executors.newWorkStealingPool();
        } else {
            int numCores = Runtime.getRuntime().availableProcessors();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(numCores * 2, numCores * 2,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            this.executor = executor;
        }
    }

    @SuppressLint("NewApi")
    public Task<List<GFCalorieDataPoint>> getCalories(Date start, Date end) {
        // NOTE: GF can silently fail on a request if response data is too large,
        // more details at https://stackoverflow.com/a/55806509/6685359
        // TODO: adjust size of chunks (duration) for the best performance
        Executor gfTaskExecutor = Executors.newFixedThreadPool(5);
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        List<Task<List<GFCalorieDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadCaloriesTask(gfTaskExecutor, dateRange);
        }).collect(Collectors.toList());
        Task<List<GFCalorieDataPoint>> getCaloriesTask = Tasks.<List<GFCalorieDataPoint>>whenAllSuccess(tasks).onSuccessTask(executor, lists -> {
            List<GFCalorieDataPoint> flattenList = lists.stream().flatMap(List::stream).collect(Collectors.toList());
            return Tasks.forResult(flattenList);
        });
        return getCaloriesTask;
    }

    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Executor gfTaskExecutor, Pair<Date, Date> dateRange) {
        return Tasks.call(gfTaskExecutor, () -> {
            for (int tryNumber = 1; tryNumber <= 5; tryNumber++) {
                Log.d(TAG, String.format("runReadCaloriesTask: awaiting #%d for %s", tryNumber, dateRange.toString()));
                try {
                    Task<List<GFCalorieDataPoint>> originalTask = readCaloriesHistory(dateRange.first, dateRange.second)
                        .onSuccessTask(executor, this::convertToCalories);
                    List<GFCalorieDataPoint> result = Tasks.await(originalTask, 60l, TimeUnit.SECONDS);
                    Log.d(TAG, String.format("runReadCaloriesTask: completed #%d for %s (size: %d)", tryNumber, dateRange.toString(),result.size()));
                    return result;
                } catch (
                    // TODO: catch unauthorized exception
                    java.util.concurrent.ExecutionException | InterruptedException | java.util.concurrent.TimeoutException exc) {
                    Log.d(TAG, String.format("runReadCaloriesTask: failed #%d for %s", tryNumber, dateRange.toString()));
                    continue;
                }
            }
            Log.d(TAG, "runReadCaloriesTask: Too many retries");
            throw new Exception("Too many retries");
//            return Tasks.forException(new Exception("Too many retries"));
        });
//            .addOnFailureListener(l -> {
//            Log.d(TAG, "addOnFailureListener: " + l.getMessage());
//        }).addOnCanceledListener(() -> {
//            Log.d(TAG, "addOnCanceledListener: ");
//        })
//            .continueWithTask(task -> {
//                if (task.isCanceled() || !task.isSuccessful()) {
//                    if (task.getException() != null) {
//                        Log.d(TAG, "Completed due to exception " + task.getException().getMessage());
//                    }
//                    Log.d(TAG, "retry for: " + dateRange);
//                    return runReadCaloriesTask(gfTaskExecutor, dateRange, tryNumber + 1);
//                }
//                Log.d(TAG, "finished for: " + dateRange);
//                return task;
//        });
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
