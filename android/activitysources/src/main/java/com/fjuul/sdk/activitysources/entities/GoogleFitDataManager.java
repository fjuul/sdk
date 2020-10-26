package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;
import androidx.core.util.Pair;

import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.runAndAwaitTaskByExecutor;
import com.fjuul.sdk.http.utils.ApiCallResult;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private static final ExecutorService localBackgroundExecutor = Executors.newCachedThreadPool();

    private GFClientWrapper client;
    private GFDataUtils gfUtils;
    private GFSyncMetadataStore gfSyncMetadataStore;


    GoogleFitDataManager(GFClientWrapper client, GFDataUtils gfUtils, GFSyncMetadataStore gfSyncMetadataStore) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
    }

    @SuppressLint("NewApi")
    public void syncIntradayMetrics(GFIntradaySyncOptions options) {
        // todo: consider returning metadata of sent data
        // add cached thread executor for local operations ?
        // add single thread executor for awaiting result
        ExecutorService sequentialExecutorService = Executors.newSingleThreadExecutor();
        LocalDate startDate = options.getStartDate();
        LocalDate endDate = options.getEndDate();
        Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getCaloriesTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getStepsTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getHRTask = Tasks.forResult(Collections.emptyList());
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        for (GFIntradaySyncOptions.METRICS_TYPE metric : options.getMetrics()) {
            switch (metric) {
                case CALORIES:
                    getCaloriesTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedCaloriesBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken);
                    break;
                case STEPS:
                    getStepsTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedStepsBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken);
                    break;
                case HEART_RATE:
                    getHRTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedHRBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken);
                    break;
            }
        }
        final Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> finalGetCaloriesTask = getCaloriesTask;
        final Task<List<GFDataPointsBatch<GFStepsDataPoint>>> finalGetStepsTask = getStepsTask;
        final Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> finalGetHRTask = getHRTask;
        final Task<GFUploadData> prepareUploadDataTask = Tasks.whenAll(getCaloriesTask, getStepsTask, getHRTask).onSuccessTask(localBackgroundExecutor, commonResult -> {
            List<GFCalorieDataPoint> calories = finalGetCaloriesTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            List<GFStepsDataPoint> steps = finalGetStepsTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            List<GFHRSummaryDataPoint> hr = finalGetHRTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            GFUploadData uploadData = new GFUploadData();
            uploadData.setCaloriesData(calories);
            uploadData.setHrData(hr);
            uploadData.setStepsData(steps);
            return Tasks.forResult(uploadData);
        });

        TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        prepareUploadDataTask.addOnCompleteListener(localBackgroundExecutor, prepareUploadDataTaskResult -> {
            if (!prepareUploadDataTaskResult.isSuccessful() || prepareUploadDataTaskResult.isCanceled()) {
                List<Task> tasks = Arrays.asList(finalGetCaloriesTask, finalGetStepsTask, finalGetHRTask);
                Optional<Exception> optionalException = GoogleTaskUtils.extractGFExceptionFromTasks(tasks);
                sendDataTaskCompletionSource.trySetException(optionalException.orElse(prepareUploadDataTaskResult.getException()));
                return;
            }
//            uploadData
            // serialize a data to json
            // TODO: send to the back-end
            sendDataTaskCompletionSource.trySetResult(ApiCallResult.<Void>value(null));
        });
        Task<ApiCallResult<Void>> sendDataTask = sendDataTaskCompletionSource.getTask();
        Task<Void> saveSyncMetadataTask = sendDataTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
            if (apiCallResult.isError()) {
                return Tasks.forException(new Exception("FAILED TO UPLOAD THE DATA"));
            }
            finalGetCaloriesTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfCalories);
            finalGetStepsTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfSteps);
            finalGetHRTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfHR);
            return Tasks.forResult(null);
        });
        saveSyncMetadataTask.addOnCompleteListener(localBackgroundExecutor, (task) -> {
            Log.d(TAG, "syncIntradayMetrics: SUCCESS: " + task.isSuccessful());
            Log.d(TAG, "syncIntradayMetrics: EXCEPTION: " + task.getException());
        });


//        return saveSyncMetadataTask;
        // todo: respond with result ?
    }

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
    private Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getNotSyncedHRBatches(LocalDate start, LocalDate end, Executor executor) {
        Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getHRSummaries(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (hr) -> {
            Duration batchDuration = Duration.ofMinutes(30);
            Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            List<GFDataPointsBatch<GFHRSummaryDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, hr, batchDuration);
            Stream<GFDataPointsBatch<GFHRSummaryDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            List<GFDataPointsBatch<GFHRSummaryDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncHRBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }
}
