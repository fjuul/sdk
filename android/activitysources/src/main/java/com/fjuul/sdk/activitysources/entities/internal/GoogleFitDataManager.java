package com.fjuul.sdk.activitysources.entities.internal;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GFIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GFSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.runAndAwaitTaskByExecutor;

import com.fjuul.sdk.core.http.utils.ApiCallResult;
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

public class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private static final ExecutorService localBackgroundExecutor = Executors.newCachedThreadPool();

    final private @NonNull
    GFClientWrapper client;
    final private @NonNull
    GFDataUtils gfUtils;
    final private @NonNull
    GFSyncMetadataStore gfSyncMetadataStore;
    final private @NonNull ActivitySourcesService activitySourcesService;

    GoogleFitDataManager(@NonNull GFClientWrapper client, @NonNull GFDataUtils gfUtils, @NonNull GFSyncMetadataStore gfSyncMetadataStore, @NonNull ActivitySourcesService activitySourcesService) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
        this.activitySourcesService = activitySourcesService;
    }

    @SuppressLint("NewApi")
    public Task<Void> syncIntradayMetrics(GFIntradaySyncOptions options) {
        // todo: consider returning metadata of the sent data
        final ExecutorService sequentialExecutorService = Executors.newSingleThreadExecutor();
        final LocalDate startDate = options.getStartDate();
        final LocalDate endDate = options.getEndDate();
        Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getCaloriesTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getStepsTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getHRTask = Tasks.forResult(Collections.emptyList());
        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final CancellationToken cancellationToken = cancellationTokenSource.getToken();
        for (FitnessMetricsType metric : options.getMetrics()) {
            switch (metric) {
                case INTRADAY_CALORIES:
                    getCaloriesTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedCaloriesBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken);
                    break;
                case INTRADAY_STEPS:
                    getStepsTask = runAndAwaitTaskByExecutor(
                        () -> getNotSyncedStepsBatches(startDate, endDate, localBackgroundExecutor),
                        sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken);
                    break;
                case INTRADAY_HEART_RATE:
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
        final Task<GFUploadData> prepareUploadDataTask = Tasks.whenAll(getCaloriesTask, getStepsTask, getHRTask).continueWithTask(localBackgroundExecutor, commonResult -> {
            if (!commonResult.isSuccessful() || commonResult.isCanceled()) {
                final List<Task> tasks = Arrays.asList(finalGetCaloriesTask, finalGetStepsTask, finalGetHRTask);
                final Optional<Exception> optionalException = GoogleTaskUtils.extractGFExceptionFromTasks(tasks);
                return Tasks.forException(optionalException.orElse(commonResult.getException()));
            }
            final List<GFCalorieDataPoint> calories = finalGetCaloriesTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            final List<GFStepsDataPoint> steps = finalGetStepsTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            final List<GFHRSummaryDataPoint> hr = finalGetHRTask.getResult().stream().flatMap(b -> b.getPoints().stream()).collect(Collectors.toList());
            final GFUploadData uploadData = new GFUploadData();
            uploadData.setCaloriesData(calories);
            uploadData.setHrData(hr);
            uploadData.setStepsData(steps);
            return Tasks.forResult(uploadData);
        });
        final Task<Void> sendDataIfNotEmptyTask = prepareUploadDataTask.onSuccessTask(localBackgroundExecutor, (uploadData) -> {
            if (uploadData.isEmpty()) {
                return Tasks.forResult(null);
            }
            return this.sendGFUploadData(uploadData).onSuccessTask(localBackgroundExecutor, (apiCallResult) -> {
                return Tasks.forResult(apiCallResult.getValue());
            });
        });
        final Task<Void> saveSyncMetadataTask = sendDataIfNotEmptyTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
            finalGetCaloriesTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfCalories);
            finalGetStepsTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfSteps);
            finalGetHRTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfHR);
            return Tasks.forResult(null);
        });
        return saveSyncMetadataTask;
    }

    @SuppressLint("NewApi")
    public Task<Void> syncSessions(GFSessionSyncOptions options) {
        final Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(options.getStartDate(), options.getEndDate());
        final Task<List<GFSessionBundle>> getNotSyncedSessionsTask = client.getSessions(gfQueryDates.first, gfQueryDates.second, options.getMinimumSessionDuration())
            .onSuccessTask(localBackgroundExecutor, sessions -> {
                final List<GFSessionBundle> notSyncedSessions = sessions.stream()
                    .filter(gfSyncMetadataStore::isNeededToSyncSessionBundle)
                    .collect(Collectors.toList());
                return Tasks.forResult(notSyncedSessions);
            });
        final Task<GFUploadData> prepareUploadDataTask = getNotSyncedSessionsTask.onSuccessTask(localBackgroundExecutor, (sessions) -> {
            final GFUploadData uploadData = new GFUploadData();
            uploadData.setSessionsData(sessions);
            return Tasks.forResult(uploadData);
        });
        final Task<Void> sendDataIfNotEmptyTask = prepareUploadDataTask.onSuccessTask(localBackgroundExecutor, (uploadData) -> {
            if (uploadData.isEmpty()) {
                return Tasks.forResult(null);
            }
            return this.sendGFUploadData(uploadData).onSuccessTask(localBackgroundExecutor, (apiCallResult) -> {
                return Tasks.forResult(apiCallResult.getValue());
            });
        });
        final Task<Void> saveSyncMetadataTask = sendDataIfNotEmptyTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
            List<GFSessionBundle> sessions = getNotSyncedSessionsTask.getResult();
            if (!sessions.isEmpty()) {
                gfSyncMetadataStore.saveSyncMetadataOfSessions(getNotSyncedSessionsTask.getResult());
            }
            return Tasks.forResult(null);
        });
        return saveSyncMetadataTask;
    }

    @SuppressLint("NewApi")
    private Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getNotSyncedCaloriesBatches(LocalDate start, LocalDate end, Executor executor) {
        final Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
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
        final Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getSteps(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (steps) -> {
            final Duration batchDuration = Duration.ofHours(6);
            final Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            final List<GFDataPointsBatch<GFStepsDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, steps, batchDuration);
            final Stream<GFDataPointsBatch<GFStepsDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            final List<GFDataPointsBatch<GFStepsDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncStepsBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @SuppressLint("NewApi")
    private Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getNotSyncedHRBatches(LocalDate start, LocalDate end, Executor executor) {
        final Pair<Date, Date> gfQueryDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        return client.getHRSummaries(gfQueryDates.first, gfQueryDates.second).onSuccessTask(executor, (hr) -> {
            final Duration batchDuration = Duration.ofMinutes(30);
            final Pair<Date, Date> batchingDates = gfUtils.adjustInputDatesForBatches(start, end, batchDuration);
            final List<GFDataPointsBatch<GFHRSummaryDataPoint>> batches = this.gfUtils.groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, hr, batchDuration);
            final Stream<GFDataPointsBatch<GFHRSummaryDataPoint>> notEmptyBatches = batches.stream().filter(b -> !b.getPoints().isEmpty());
            final List<GFDataPointsBatch<GFHRSummaryDataPoint>> notSyncedBatches = notEmptyBatches
                .filter(this.gfSyncMetadataStore::isNeededToSyncHRBatch)
                .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    private Task<ApiCallResult<Void>> sendGFUploadData(GFUploadData uploadData) {
        final TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        activitySourcesService.uploadGoogleFitData(uploadData).enqueue((apiCall, result) -> {
            if (result.isError()) {
                final CommonException exception = new CommonException("Failed to send data to the server", result.getError());
                sendDataTaskCompletionSource.trySetException(exception);
                return;
            }
            sendDataTaskCompletionSource.trySetResult(result);
        });
        return sendDataTaskCompletionSource.getTask();
    }
}