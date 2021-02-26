package com.fjuul.sdk.activitysources.entities.internal;

import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.runAndAwaitTaskByExecutor;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFWeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.core.utils.Logger;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;


public class GFDataManager {
    private static final ExecutorService localBackgroundExecutor = Executors.newCachedThreadPool();

    final private @NonNull GFClientWrapper client;
    final private @NonNull GFDataUtils gfUtils;
    final private @NonNull GFSyncMetadataStore gfSyncMetadataStore;
    final private @NonNull ActivitySourcesService activitySourcesService;
    final private @Nullable Date lowerDateBoundary;

    private final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Nullable
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format;
        }
    };

    GFDataManager(@NonNull GFClientWrapper client,
        @NonNull GFDataUtils gfUtils,
        @NonNull GFSyncMetadataStore gfSyncMetadataStore,
        @NonNull ActivitySourcesService activitySourcesService,
        @Nullable Date lowerDateBoundary) {
        this.client = client;
        this.gfUtils = gfUtils;
        this.gfSyncMetadataStore = gfSyncMetadataStore;
        this.activitySourcesService = activitySourcesService;
        this.lowerDateBoundary = lowerDateBoundary;
    }

    @SuppressLint("NewApi")
    @NonNull
    public Task<Void> syncIntradayMetrics(@NonNull GoogleFitIntradaySyncOptions options) {
        // todo: consider returning metadata of the sent data
        final Pair<Date, Date> queryDates = transformInputDates(options.getStartDate(), options.getEndDate());
        final Date startDate = queryDates.first;
        final Date endDate = queryDates.second;
        if (startDate.equals(endDate)) {
            // in other words, if the duration gap between two input dates is zero then no make sense to sync any data
            Logger.get()
                .d("skip syncing GF intraday metrics (%s) with input dates [%s, %s]",
                    options.getMetrics().stream().map(metric -> metric.toString()).collect(Collectors.joining(", ")),
                    options.getStartDate().toString(),
                    options.getEndDate().toString());
            return Tasks.forResult(null);
        }
        Logger.get()
            .d("start syncing GF intraday metrics (%s) with date range [%s, %s]",
                options.getMetrics().stream().map(metric -> metric.toString()).collect(Collectors.joining(", ")),
                dateFormatter.get().format(startDate),
                dateFormatter.get().format(endDate));
        final ExecutorService sequentialExecutorService = Executors.newSingleThreadExecutor();
        Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getCaloriesTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getStepsTask = Tasks.forResult(Collections.emptyList());
        Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getHRTask = Tasks.forResult(Collections.emptyList());
        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final CancellationToken cancellationToken = cancellationTokenSource.getToken();
        for (FitnessMetricsType metric : options.getMetrics()) {
            switch (metric) {
                case INTRADAY_CALORIES:
                    getCaloriesTask = runAndAwaitTaskByExecutor(sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken,
                        () -> getNotSyncedCaloriesBatches(startDate, endDate, localBackgroundExecutor));
                    break;
                case INTRADAY_STEPS:
                    getStepsTask = runAndAwaitTaskByExecutor(sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken,
                        () -> getNotSyncedStepsBatches(startDate, endDate, localBackgroundExecutor));
                    break;
                case INTRADAY_HEART_RATE:
                    getHRTask = runAndAwaitTaskByExecutor(sequentialExecutorService,
                        cancellationTokenSource,
                        cancellationToken,
                        () -> getNotSyncedHRBatches(startDate, endDate, localBackgroundExecutor));
                    break;
            }
        }
        final Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> finalGetCaloriesTask = getCaloriesTask;
        final Task<List<GFDataPointsBatch<GFStepsDataPoint>>> finalGetStepsTask = getStepsTask;
        final Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> finalGetHRTask = getHRTask;
        final Task<GFUploadData> prepareUploadDataTask = Tasks.whenAll(getCaloriesTask, getStepsTask, getHRTask)
            .continueWithTask(localBackgroundExecutor, commonResult -> {
                if (!commonResult.isSuccessful() || commonResult.isCanceled()) {
                    final List<Task> tasks = Arrays.asList(finalGetCaloriesTask, finalGetStepsTask, finalGetHRTask);
                    final Optional<Exception> optionalException = GoogleTaskUtils.extractGFExceptionFromTasks(tasks);
                    return Tasks.forException(optionalException.orElse(commonResult.getException()));
                }
                final List<GFCalorieDataPoint> calories = finalGetCaloriesTask.getResult()
                    .stream()
                    .flatMap(b -> b.getPoints().stream())
                    .collect(Collectors.toList());
                final List<GFStepsDataPoint> steps = finalGetStepsTask.getResult()
                    .stream()
                    .flatMap(b -> b.getPoints().stream())
                    .collect(Collectors.toList());
                final List<GFHRSummaryDataPoint> hr = finalGetHRTask.getResult()
                    .stream()
                    .flatMap(b -> b.getPoints().stream())
                    .collect(Collectors.toList());
                final GFUploadData uploadData = new GFUploadData();
                uploadData.setCaloriesData(calories);
                uploadData.setHrData(hr);
                uploadData.setStepsData(steps);
                return Tasks.forResult(uploadData);
            });
        final Task<Void> sendDataIfNotEmptyTask =
            prepareUploadDataTask.onSuccessTask(localBackgroundExecutor, (uploadData) -> {
                if (uploadData.isEmpty()) {
                    Logger.get().d("no new data to send");
                    return Tasks.forResult(null);
                }
                Logger.get().d("sending new GF data: %s", uploadData.toString());
                return this.sendGFUploadData(uploadData).onSuccessTask(localBackgroundExecutor, (apiCallResult) -> {
                    return Tasks.forResult(apiCallResult.getValue());
                });
            });
        final Task<Void> saveSyncMetadataTask =
            sendDataIfNotEmptyTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
                finalGetCaloriesTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfCalories);
                finalGetStepsTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfSteps);
                finalGetHRTask.getResult().forEach(gfSyncMetadataStore::saveSyncMetadataOfHR);
                return Tasks.forResult(null);
            });
        return saveSyncMetadataTask;
    }

    @SuppressLint("NewApi")
    @NonNull
    public Task<Void> syncSessions(@NonNull GoogleFitSessionSyncOptions options) {
        final Pair<Date, Date> queryDates = transformInputDates(options.getStartDate(), options.getEndDate());
        final Date startDate = queryDates.first;
        final Date endDate = queryDates.second;
        if (startDate.equals(endDate)) {
            // in other words, if the duration gap between two input dates is zero then no make sense to sync any data
            Logger.get()
                .d("skip syncing GF sessions with input dates [%s, %s]",
                    options.getStartDate().toString(),
                    options.getEndDate().toString());
            return Tasks.forResult(null);
        }

        Logger.get()
            .d("start syncing GF sessions with date range [%s, %s]",
                dateFormatter.get().format(startDate),
                dateFormatter.get().format(endDate));
        final Task<List<GFSessionBundle>> getNotSyncedSessionsTask =
            client.getSessions(startDate, endDate, options.getMinimumSessionDuration())
                .onSuccessTask(localBackgroundExecutor, sessions -> {
                    final List<GFSessionBundle> notSyncedSessions = sessions.stream()
                        .filter(gfSyncMetadataStore::isNeededToSyncSessionBundle)
                        .collect(Collectors.toList());
                    return Tasks.forResult(notSyncedSessions);
                });
        final Task<GFUploadData> prepareUploadDataTask =
            getNotSyncedSessionsTask.onSuccessTask(localBackgroundExecutor, (sessions) -> {
                final GFUploadData uploadData = new GFUploadData();
                uploadData.setSessionsData(sessions);
                return Tasks.forResult(uploadData);
            });
        final Task<Void> sendDataIfNotEmptyTask =
            prepareUploadDataTask.onSuccessTask(localBackgroundExecutor, (uploadData) -> {
                if (uploadData.isEmpty()) {
                    Logger.get().d("no new data to send");
                    return Tasks.forResult(null);
                }
                Logger.get().d("sending new GF data: %s", uploadData.toString());
                return this.sendGFUploadData(uploadData).onSuccessTask(localBackgroundExecutor, (apiCallResult) -> {
                    return Tasks.forResult(apiCallResult.getValue());
                });
            });
        final Task<Void> saveSyncMetadataTask =
            sendDataIfNotEmptyTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
                List<GFSessionBundle> sessions = getNotSyncedSessionsTask.getResult();
                if (!sessions.isEmpty()) {
                    gfSyncMetadataStore.saveSyncMetadataOfSessions(getNotSyncedSessionsTask.getResult());
                }
                return Tasks.forResult(null);
            });
        return saveSyncMetadataTask;
    }

    @SuppressLint("NewApi")
    @NonNull
    public Task<Boolean> syncProfile(@NonNull GoogleFitProfileSyncOptions options) {
        Logger.get()
            .d("start syncing GF profile metrics (%s)",
                options.getMetrics().stream().map(metric -> metric.toString()).collect(Collectors.joining(", ")));
        final Task<GFWeightDataPoint> getWeightTask =
            options.getMetrics().contains(FitnessMetricsType.WEIGHT) ? getNotSyncedWeight(localBackgroundExecutor)
                : Tasks.forResult(null);
        final Task<GFHeightDataPoint> getHeightTask =
            options.getMetrics().contains(FitnessMetricsType.HEIGHT) ? getNotSyncedHeight(localBackgroundExecutor)
                : Tasks.forResult(null);
        final List<Task<?>> allTasks = Arrays.asList(getHeightTask, getWeightTask);
        final Task<GFSynchronizableProfileParams> prepareProfileParamsTask =
            Tasks.whenAll(allTasks).continueWithTask(localBackgroundExecutor, commonResult -> {
                if (!commonResult.isSuccessful() || commonResult.isCanceled()) {
                    final Optional<Exception> optionalException = GoogleTaskUtils.extractGFExceptionFromTasks(allTasks);
                    return Tasks.forException(optionalException.orElse(commonResult.getException()));
                }
                final GFWeightDataPoint weightDataPoint = getWeightTask.getResult();
                final GFHeightDataPoint heightDataPoint = getHeightTask.getResult();

                final GFSynchronizableProfileParams profileParams = new GFSynchronizableProfileParams();
                if (weightDataPoint != null) {
                    profileParams.setWeight(weightDataPoint.getValue());
                }
                if (heightDataPoint != null) {
                    profileParams.setHeight(heightDataPoint.getValue());
                }
                return Tasks.forResult(profileParams);
            });

        final Task<Void> sendDataIfNotEmptyTask =
            prepareProfileParamsTask.onSuccessTask(localBackgroundExecutor, (profileParams) -> {
                if (profileParams.isEmpty()) {
                    Logger.get().d("no the updated profile parameters to send");
                    return Tasks.forResult(null);
                }
                Logger.get().d("sending the updated profile parameters: %s", profileParams.toString());
                return this.sendGFProfileParams(profileParams)
                    .onSuccessTask(localBackgroundExecutor, (apiCallResult -> {
                        return Tasks.forResult(apiCallResult.getValue());
                    }));
            });
        final Task<Boolean> saveSyncMetadataTask =
            sendDataIfNotEmptyTask.onSuccessTask(localBackgroundExecutor, apiCallResult -> {
                boolean changed = false;
                final GFWeightDataPoint weightDataPoint = getWeightTask.getResult();
                if (weightDataPoint != null) {
                    gfSyncMetadataStore.saveSyncMetadataOfWeight(weightDataPoint);
                    changed = true;
                }
                final GFHeightDataPoint heightDataPoint = getHeightTask.getResult();
                if (heightDataPoint != null) {
                    gfSyncMetadataStore.saveSyncMetadataOfHeight(heightDataPoint);
                    changed = true;
                }
                return Tasks.forResult(changed);
            });
        return saveSyncMetadataTask;
    }

    @SuppressLint("NewApi")
    @NonNull
    private Task<List<GFDataPointsBatch<GFCalorieDataPoint>>> getNotSyncedCaloriesBatches(@NonNull Date start,
        @NonNull Date end,
        @NonNull Executor executor) {
        return client.getCalories(start, end).onSuccessTask(executor, (calories) -> {
            final Duration batchDuration = Duration.ofMinutes(30);
            final Pair<Date, Date> batchingDates = gfUtils.roundDatesByIntradayBatchDuration(start, end, batchDuration);
            final List<GFDataPointsBatch<GFCalorieDataPoint>> batches = this.gfUtils
                .groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, calories, batchDuration);
            final Stream<GFDataPointsBatch<GFCalorieDataPoint>> notEmptyBatches =
                batches.stream().filter(b -> !b.getPoints().isEmpty());
            final List<GFDataPointsBatch<GFCalorieDataPoint>> notSyncedBatches =
                notEmptyBatches.filter(this.gfSyncMetadataStore::isNeededToSyncCaloriesBatch)
                    .collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @SuppressLint("NewApi")
    @NonNull
    private Task<List<GFDataPointsBatch<GFStepsDataPoint>>> getNotSyncedStepsBatches(@NonNull Date start,
        @NonNull Date end,
        @NonNull Executor executor) {
        return client.getSteps(start, end).onSuccessTask(executor, (steps) -> {
            final Duration batchDuration = Duration.ofHours(6);
            final Pair<Date, Date> batchingDates = gfUtils.roundDatesByIntradayBatchDuration(start, end, batchDuration);
            final List<GFDataPointsBatch<GFStepsDataPoint>> batches = this.gfUtils
                .groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, steps, batchDuration);
            final Stream<GFDataPointsBatch<GFStepsDataPoint>> notEmptyBatches =
                batches.stream().filter(b -> !b.getPoints().isEmpty());
            final List<GFDataPointsBatch<GFStepsDataPoint>> notSyncedBatches =
                notEmptyBatches.filter(this.gfSyncMetadataStore::isNeededToSyncStepsBatch).collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @SuppressLint("NewApi")
    @NonNull
    private Task<List<GFDataPointsBatch<GFHRSummaryDataPoint>>> getNotSyncedHRBatches(@NonNull Date start,
        @NonNull Date end,
        @NonNull Executor executor) {
        return client.getHRSummaries(start, end).onSuccessTask(executor, (hr) -> {
            final Duration batchDuration = Duration.ofMinutes(30);
            final Pair<Date, Date> batchingDates = gfUtils.roundDatesByIntradayBatchDuration(start, end, batchDuration);
            final List<GFDataPointsBatch<GFHRSummaryDataPoint>> batches = this.gfUtils
                .groupPointsIntoBatchesByDuration(batchingDates.first, batchingDates.second, hr, batchDuration);
            final Stream<GFDataPointsBatch<GFHRSummaryDataPoint>> notEmptyBatches =
                batches.stream().filter(b -> !b.getPoints().isEmpty());
            final List<GFDataPointsBatch<GFHRSummaryDataPoint>> notSyncedBatches =
                notEmptyBatches.filter(this.gfSyncMetadataStore::isNeededToSyncHRBatch).collect(Collectors.toList());
            return Tasks.forResult(notSyncedBatches);
        });
    }

    @NonNull
    private Task<GFWeightDataPoint> getNotSyncedWeight(@NonNull Executor executor) {
        return client.getLastKnownWeight().onSuccessTask(executor, (weightDataPoint) -> {
            if (weightDataPoint == null || !this.gfSyncMetadataStore.isNeededToSyncWeight(weightDataPoint)) {
                return Tasks.forResult(null);
            }
            return Tasks.forResult(weightDataPoint);
        });
    }

    @NonNull
    private Task<GFHeightDataPoint> getNotSyncedHeight(@NonNull Executor executor) {
        return client.getLastKnownHeight().onSuccessTask(executor, (heightDataPoint) -> {
            if (heightDataPoint == null || !this.gfSyncMetadataStore.isNeededToSyncHeight(heightDataPoint)) {
                return Tasks.forResult(null);
            }
            return Tasks.forResult(heightDataPoint);
        });
    }

    @NonNull
    private Task<ApiCallResult<Void>> sendGFUploadData(@NonNull GFUploadData uploadData) {
        final TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        activitySourcesService.uploadGoogleFitData(uploadData).enqueue((apiCall, result) -> {
            if (result.isError()) {
                Logger.get().d("failed to send GF data: %s", result.getError().getMessage());
                final CommonException exception =
                    new CommonException("Failed to send data to the server", result.getError());
                sendDataTaskCompletionSource.trySetException(exception);
                return;
            }
            Logger.get().d("succeeded to send GF data");
            sendDataTaskCompletionSource.trySetResult(result);
        });
        return sendDataTaskCompletionSource.getTask();
    }

    @NonNull
    private Task<ApiCallResult<Void>> sendGFProfileParams(@NonNull GFSynchronizableProfileParams profileParams) {
        final TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        activitySourcesService.updateProfileOnBehalfOfGoogleFit(profileParams).enqueue((apiCall, result) -> {
            if (result.isError()) {
                Logger.get().d("failed to send the profile data: %s", result.getError().getMessage());
                final CommonException exception =
                    new CommonException("Failed to send data to the server", result.getError());
                sendDataTaskCompletionSource.trySetException(exception);
                return;
            }
            Logger.get().d("succeeded to send the profile data");
            sendDataTaskCompletionSource.trySetResult(result);
        });
        return sendDataTaskCompletionSource.getTask();
    }

    private Date correctDateByLowerBoundary(@NonNull Date date) {
        if (lowerDateBoundary == null) {
            return date;
        }
        return date.before(lowerDateBoundary) ? lowerDateBoundary : date;
    }

    private Pair<Date, Date> transformInputDates(@NonNull LocalDate start, @NonNull LocalDate end) {
        final Pair<Date, Date> gfInputDates = gfUtils.adjustInputDatesForGFRequest(start, end);
        final Date startDate = correctDateByLowerBoundary(gfInputDates.first);
        final Date endDate = correctDateByLowerBoundary(gfInputDates.second);
        return new Pair<>(startDate, endDate);
    }
}
