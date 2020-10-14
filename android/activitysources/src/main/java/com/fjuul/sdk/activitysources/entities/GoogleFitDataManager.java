package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.core.util.Pair;
import androidx.core.util.Supplier;

import com.fjuul.sdk.http.utils.ApiCallResult;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private static final ExecutorService localBackgroundExecutor = Executors.newCachedThreadPool();

    private GFClientWrapper client;
    private GFDataUtils gfUtils;
    private GFSyncMetadataStore gfSyncMetadataStore;


    public GoogleFitDataManager(GFClientWrapper client, GFDataUtils gfUtils, GFSyncMetadataStore gfSyncMetadataStore) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
    }

    @SuppressLint("NewApi")
    Task<Void> syncIntradayMetrics(GFIntradaySyncOptions options) {
        // todo: consider returning metadata of sent data
        // todo: test cancellation for calories!
        // add cached thread executor for local operations ?
        // add single thread executor for awaiting result
        ExecutorService sequentialExecutorService = Executors.newSingleThreadExecutor();
        LocalDate startDate = options.getStartDate();
        LocalDate endDate = options.getEndDate();
        Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getCaloriesTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getStepsTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFHRDataPoint>>> getHRTask = Tasks.forResult(Collections.emptyList());
        for (GFIntradaySyncOptions.METRICS_TYPE metric : options.getMetrics()) {
            switch (metric) {
                case CALORIES:
                    getCaloriesTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedCaloriesBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService);
                    break;
                case STEPS:
                    getStepsTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedStepsBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService);
                    break;
                case HR:
                    getHRTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedHRBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService);
                    break;
            }
        }
        final Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> finalGetCaloriesTask = getCaloriesTask;
        final Task<List<GFDataPointsBatch<GFStepsDataPoint>>> finalGetStepsTask = getStepsTask;
        final Task<List<GFDataPointsBatch<GFHRDataPoint>>> finalGetHRTask = getHRTask;
        final Task<GFUploadData> prepareUploadDataTask = Tasks.whenAll(getCaloriesTask, getStepsTask, getHRTask).onSuccessTask(localBackgroundExecutor, commonResult -> {
            // TODO: infer right exception here
//            if (!commonResult.isSuccessful() || !commonResult.isCanceled()) {
//                return Tasks.forException(new Exception("TASK WAS FAILED"));
//            }
            List<GFCalorieDataPoint> calories = finalGetCaloriesTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            List<GFStepsDataPoint> steps = finalGetStepsTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            List<GFHRDataPoint> hr = finalGetHRTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            GFUploadData uploadData = new GFUploadData();
            uploadData.setCaloriesData(calories);
            uploadData.setHrData(hr);
            uploadData.setStepsData(steps);
            return Tasks.forResult(uploadData);
        });
        TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        prepareUploadDataTask.addOnSuccessListener(localBackgroundExecutor, uploadData -> {
//            uploadData
            // serialize a data to json
            // TODO: send to the back-end
            sendDataTaskCompletionSource.trySetResult(ApiCallResult.<Void>value(null));
        });
        Task<ApiCallResult<Void>> sendDataTask = sendDataTaskCompletionSource.getTask();
        Task<Void> saveSyncMetadataTask = sendDataTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
            if (!apiCallResult.isError()) {
                return Tasks.forException(new Exception("FAILED TO UPLOAD THE DATA"));
            }
            finalGetCaloriesTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfCalories);
            finalGetStepsTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfSteps);
            finalGetHRTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfHR);
            return Tasks.forResult(null);
        });
        return saveSyncMetadataTask;
        // todo: respond with result ?
    }

    @SuppressLint("NewApi")
    private Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getNotSyncedCaloriesBatches(LocalDate start, LocalDate end, Executor executor) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getCalories(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (calories) -> {
            Duration batchDuration = Duration.ofMinutes(30);
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, calories, batchDuration);
            Stream<GFDataPointsBatch<GFCalorieDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @SuppressLint("NewApi")
    private Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getNotSyncedStepsBatches(LocalDate start, LocalDate end, Executor executor) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getSteps(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (steps) -> {
            Duration batchDuration = Duration.ofHours(6);
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            List<GFDataPointsBatch<GFStepsDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, steps, batchDuration);
            Stream<GFDataPointsBatch<GFStepsDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFStepsDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncStepsBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @SuppressLint("NewApi")
    private Task<List<GFDataPointsBatch<GFHRDataPoint>>> getNotSyncedHRBatches(LocalDate start, LocalDate end, Executor executor) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getHRs(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (hr) -> {
            Duration batchDuration = Duration.ofMinutes(30);
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            List<GFDataPointsBatch<GFHRDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, hr, batchDuration);
            Stream<GFDataPointsBatch<GFHRDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFHRDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncHRBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    // TODO: refactoring: extract this method to some thread utils class
    static <T> Task<T> runAndAwaitTaskByExecutor(Supplier<Task<T>> taskSupplier, Executor executor) {
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(cancellationTokenSource.getToken());
        executor.execute(() -> {
            try {
                T result = Tasks.await(taskSupplier.get());
                taskCompletionSource.trySetResult(result);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    taskCompletionSource.trySetException((Exception)e.getCause());
                } else {
                    taskCompletionSource.trySetException(e);
                }
            } catch (InterruptedException e) {
                cancellationTokenSource.cancel();
            }
        });
        return taskCompletionSource.getTask();
    }

//    @SuppressLint("NewApi")
//    public void syncCalories(LocalDate start, LocalDate end) {
//        // TODO: throw if end is future!
//        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
//        client.getCalories(gfQueryDates.first, gfQueryDates.second).continueWith((getCaloriesTask) -> {
//            Log.d(TAG, "syncCalories: DONE = " + getCaloriesTask.isSuccessful());
//            if (!getCaloriesTask.isSuccessful()) {
//                // TODO: invoke callback with exception
////                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
//                Log.d(TAG, "Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
//                return null;
//            }
//            for (GFCalorieDataPoint calorie : getCaloriesTask.getResult()) {
//                Log.d(TAG, "Calorie " + calorie);
//            }
////            Log.d(TAG, "syncCalories: DONE");
//            List<GFCalorieDataPoint> calories = getCaloriesTask.getResult();
//            Duration batchDuration = Duration.ofMinutes(30);
//            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
//            List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, calories, batchDuration);
//            Stream<GFDataPointsBatch<GFCalorieDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
//            List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches = notEmptyBatches
//                .filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
//                .collect(Collectors.toList());
//            if (notSyncedBatches.isEmpty()) {
//                // TODO: invoke callback with no result (or empty metadata) ?
//                return null;
//            }
//            // TODO: send the data to the back-end side => add service for that (consider retry here)
//            notSyncedBatches.forEach(batch -> {
//                this.gfSyncMetadataStore.saveSyncMetadataOfCalories(batch);
//            });
//            // TODO: pass metadata to the callback ???
//            return null;
//        });
//    }
//
//    @SuppressLint("NewApi")
//    public void syncSteps(LocalDate start, LocalDate end) {
//        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
//        client.getSteps(gfQueryDates.first, gfQueryDates.second).continueWith((getStepsTask) -> {
//            Log.d(TAG, "syncSteps: DONE = " + getStepsTask.isSuccessful());
//            if (!getStepsTask.isSuccessful()) {
//                // TODO: invoke callback with exception
////                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
//                Log.d(TAG, "Couldn't get steps from GoogleFit Api: " + getStepsTask.getException().getMessage());
//                return null;
//            }
//            List<GFStepsDataPoint> steps = getStepsTask.getResult();
//            Duration batchDuration = Duration.ofHours(6);
//            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
//            List<GFDataPointsBatch<GFStepsDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, steps, batchDuration);
//            Stream<GFDataPointsBatch<GFStepsDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
//            List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches = notEmptyBatches
//                .filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
//                .collect(Collectors.toList());
//            Pair<Date, Date>
//           return null;
//        });
//    }
//
//    public void syncHR(LocalDate start, LocalDate end) {
//        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
//        client.getHRs(gfQueryDates.first, gfQueryDates.second).continueWith((getHRTask) -> {
//            Log.d(TAG, "syncHR: DONE = " + getHRTask.isSuccessful());
//            if (!getHRTask.isSuccessful()) {
//                // TODO: invoke callback with exception
////                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
//                Log.d(TAG, "Couldn't get HRs from GoogleFit Api: " + getHRTask.getException().getMessage());
//                return null;
//            }
//            Log.d(TAG, "syncHR: TOTAL SIZE: " + getHRTask.getResult().size());
//            return null;
//        });
//    }

    public void syncSessions(LocalDate start, LocalDate end, Duration minimumSessionDuration) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        client.getSessions(gfQueryDates.first, gfQueryDates.second, minimumSessionDuration).continueWith(task -> {
            Log.d(TAG, "syncSessions: DONE = " + task.isSuccessful());
            if (!task.isSuccessful()) {
                // TODO: invoke callback with exception
//                throw new Error("Couldn't get calories from GoogleFit Api: " + getCaloriesTask.getException().getMessage());
                Log.d(TAG, "Couldn't get Sessions from GoogleFit Api: " + task.getException().getMessage());
                return null;
            }
            Log.d(TAG, "syncSessions: TOTAL SIZE: " + task.getResult().size());
            for (GFSessionBundle s : task.getResult()) {
                Log.d(TAG, "syncSessions: SESSION " + s);
                for (GFPowerDataPoint power : s.getPower()) {
                    Log.d(TAG, "syncSessions: power " + power);
                }
                for (GFSpeedDataPoint speed : s.getSpeed()) {
                    Log.d(TAG, "syncSessions: speed " + speed);
                }
            }
            return null;
        });
    }
}
