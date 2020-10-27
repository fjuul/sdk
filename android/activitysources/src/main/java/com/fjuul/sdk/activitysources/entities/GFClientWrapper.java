package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.core.util.Pair;
import androidx.core.util.Supplier;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions.MaxRetriesExceededException;
import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.shutdownExecutorsOnComplete;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// NOTE: a package below it's not supposed to be used as public api but I didn't find another way to
// get the integer based constant of ActivityType by the string presentation which is returned by default
// along with a session.
import com.google.android.gms.internal.fitness.zzjr;

// NOTE: GF can silently fail on a request if response data is too large, more details at https://stackoverflow.com/a/55806509/6685359
// Also there may be a case when GoogleFit service can't response to the requester on the first attempts due to failed delivery.
// Therefore the wrapper divide one big request into smaller ones and use a fixed thread pool to watch for the fired requests with a timeout and retries.

public final class GFClientWrapper {
    private static final String TAG = "GFClientWrapper";
    private static final int RETRIES_COUNT = 5;
    private static final int GF_TASK_WATCHER_THREAD_POOL_SIZE = 5;
    private static final long GF_QUERY_TIMEOUT_SECONDS = 60l;

    private HistoryClient historyClient;
    private SessionsClient sessionsClient;
    private GFDataUtils gfUtils;
    // TODO: rename executor
    private Executor executor;

    public GFClientWrapper(HistoryClient historyClient, SessionsClient sessionsClient, GFDataUtils gfUtils) {
        this.historyClient = historyClient;
        this.sessionsClient = sessionsClient;
        this.gfUtils = gfUtils;
        // TODO: choose the best executor for this wrapper needs (gf data converting)
        executor = Executors.newCachedThreadPool();
    }

    @SuppressLint("NewApi")
    public Task<List<GFCalorieDataPoint>> getCalories(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFCalorieDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadCaloriesTask(dateRange, gfTaskWatcher);
        }).collect(Collectors.toList());

        Task<List<GFCalorieDataPoint>> getCaloriesTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(executor, getCaloriesTask, gfTaskWatcherExecutor);
        // TODO: check if it was really shutdown after a while
    }

    @SuppressLint("NewApi")
    public Task<List<GFStepsDataPoint>> getSteps(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofDays(12));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFStepsDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadStepsTask(dateRange, gfTaskWatcher);
        }).collect(Collectors.toList());

        Task<List<GFStepsDataPoint>> getStepsTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(executor, getStepsTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<List<GFHRSummaryDataPoint>> getHRSummaries(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcherDetails = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFHRSummaryDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadHRTask(dateRange, Duration.ofMinutes(1), gfTaskWatcherDetails);
        }).collect(Collectors.toList());

        Task<List<GFHRSummaryDataPoint>> getHRTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(executor, getHRTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<List<GFSessionBundle>> getSessions(Date start, Date end, Duration minSessionDuration) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        ExecutorService gfSubTaskWatcherExecutor = createGfTaskWatcherExecutor();
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        SupervisedExecutor gfReadSessionsWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        SupervisedExecutor gfReadSessionSamplesWatcher = new SupervisedExecutor(gfSubTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofDays(5));

        List<Task<List<GFSessionBundle>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadSessions(dateRange, minSessionDuration, gfReadSessionsWatcher, gfReadSessionSamplesWatcher);
        }).collect(Collectors.toList());

        Task<List<GFSessionBundle>> getSessionsTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(executor, getSessionsTask, gfTaskWatcherExecutor, gfSubTaskWatcherExecutor);
        // TODO: check if sub-task watcher was shutdown
    }

    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
        Supplier<Task<List<GFCalorieDataPoint>>> taskSupplier = () -> {
            return readCaloriesHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, this::convertDataReadResponseToCalories);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    private Task<List<GFStepsDataPoint>> runReadStepsTask(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
        Supplier<Task<List<GFStepsDataPoint>>> taskSupplier = () -> {
            return readStepsHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, this::convertDataReadResponseToSteps);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    @SuppressLint("NewApi")
    private Task<List<GFHRSummaryDataPoint>> runReadHRTask(Pair<Date, Date> dateRange, Duration bucketDuration, SupervisedExecutor gfTaskWatcher) {
        Supplier<Task<List<GFHRSummaryDataPoint>>> taskSupplier = () -> {
            return readHRHistory(dateRange.first, dateRange.second, bucketDuration)
                .onSuccessTask(executor, this::convertDataReadResponseToHRSummaries);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    private Task<List<GFSessionBundle>> runReadSessions(Pair<Date, Date> dateRange, Duration minSessionDuration, SupervisedExecutor gfTaskWatcher, SupervisedExecutor gfSubTaskWatcher) {
        Supplier<Task<SessionReadResponse>> taskSupplier = () -> readSessions(dateRange.first, dateRange.second);
        Task<SessionReadResponse> readRawSessionsUnderWatchTask = runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
        Task<List<GFSessionBundle>> bundledSessionsTask = readRawSessionsUnderWatchTask.onSuccessTask(gfTaskWatcher.getExecutor(), (readResponse) -> {
            return bundleSessionsWithData(readResponse, minSessionDuration, gfSubTaskWatcher);
        });
        return bundledSessionsTask;
    }

    private <T> Task<T> runGFTaskUnderWatch(Supplier<Task<T>> taskSupplier, SupervisedExecutor gfTaskWatcher) {
        ExecutorService gfTaskWatcherExecutor = gfTaskWatcher.getExecutor();
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(gfTaskWatcher.getCancellationToken());
        CancellationTokenSource cancellationTokenSource = gfTaskWatcher.getCancellationTokenSource();
        CancellationToken cancellationToken = gfTaskWatcher.getCancellationToken();
        gfTaskWatcherExecutor.execute(() -> {
            for (int tryNumber = 1; tryNumber <= RETRIES_COUNT && !cancellationToken.isCancellationRequested(); tryNumber++) {
//                Log.d(TAG, String.format("runGFTaskUnderWatch: awaiting #%d", tryNumber));
                try {
                    Task<T> originalTask = taskSupplier.get();
                    T result = Tasks.await(originalTask, GF_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
//                    Log.d(TAG, String.format("runGFTaskUnderWatch: completed #%d", tryNumber));
                    taskCompletionSource.trySetResult(result);
                    return;
                } catch (
                    // TODO: catch unauthorized exception
                    java.util.concurrent.ExecutionException | InterruptedException | java.util.concurrent.TimeoutException exc) {
//                    Log.d(TAG, String.format("runGFTaskUnderWatch: failed #%d", tryNumber));
                    continue;
                }
            }
            if (cancellationToken.isCancellationRequested()) {
                return;
            }
            // Log.d(TAG, "runGFTaskUnderWatch: Too many retries");
            // TODO: introduce new class for gf task parameters
            taskCompletionSource.trySetException(new MaxRetriesExceededException("Too many retries"));
            cancellationTokenSource.cancel();
        });
        return taskCompletionSource.getTask();
    }

    @SuppressLint("NewApi")
    private <V, T extends List<V>> Task<T> flatMapTasksResults(List<Task<T>> tasks) {
        Task<T> collectResultsTask = Tasks.whenAll(tasks).continueWithTask(executor, commonResult -> {
            if (commonResult.isCanceled() || !commonResult.isSuccessful()) {
                CommonException fallbackException = new CommonException("Pooling task was canceled");
                Exception exception = GoogleTaskUtils.extractGFExceptionFromTasks(tasks).orElse(fallbackException);;
                return Tasks.forException(exception);
            }
            T flattenResults = (T) tasks.stream()
                .flatMap(t -> t.getResult().stream())
                .collect(Collectors.toList());
            return Tasks.forResult(flattenResults);
        });
        return collectResultsTask;
    }

    private Task<List<GFCalorieDataPoint>> convertDataReadResponseToCalories(DataReadResponse dataReadResponse) {
        ArrayList<GFCalorieDataPoint> calorieDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
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

    private GFCalorieDataPoint convertDataPointToCalorie(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_CALORIES_EXPENDED.getFields()) {
            if (Field.FIELD_CALORIES.equals(field)) {
                float kcals = dataPoint.getValue(field).asFloat();
                return new GFCalorieDataPoint(kcals, start, end, dataSourceId);
            }
        }
        return null;
    }

    private Task<List<GFStepsDataPoint>> convertDataReadResponseToSteps(DataReadResponse dataReadResponse) {
        ArrayList<GFStepsDataPoint> stepsDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
            DataPoint stepsDataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = stepsDataPoint.getOriginalDataSource().getStreamIdentifier();
            for (Field field : DataType.TYPE_STEP_COUNT_DELTA.getFields()) {
                if (Field.FIELD_STEPS.equals(field)) {
                    int steps = stepsDataPoint.getValue(field).asInt();
                    GFStepsDataPoint convertedStepsDataPoint = new GFStepsDataPoint(steps, start, dataSourceId);
                    stepsDataPoints.add(convertedStepsDataPoint);
                }
            }
        }
        return Tasks.forResult(stepsDataPoints);
    }

    private GFStepsDataPoint convertDataPointToSteps(DataPoint dataPoint) {
        Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        Date end = new Date(dataPoint.getEndTime(TimeUnit.MILLISECONDS));
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        for (Field field : DataType.TYPE_STEP_COUNT_DELTA.getFields()) {
            if (Field.FIELD_STEPS.equals(field)) {
                int steps = dataPoint.getValue(field).asInt();
                return new GFStepsDataPoint(steps, start, end, dataSourceId);
            }
        }
        return null;
    }

    private Task<List<GFHRSummaryDataPoint>> convertDataReadResponseToHRSummaries(DataReadResponse dataReadResponse) {
        ArrayList<GFHRSummaryDataPoint> hrDataPoints = new ArrayList<>();
        for (Bucket bucket : dataReadResponse.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
            if (bucket.getDataSets().isEmpty()) {
                continue;
            }
            DataSet dataSet = bucket.getDataSets().get(0);
            if (dataSet.isEmpty()) {
                continue;
            }
            DataPoint dataPoint = dataSet.getDataPoints().get(0);
            String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
            float avgBPM = dataPoint.getValue(Field.FIELD_AVERAGE).asFloat();
            float minBPM = dataPoint.getValue(Field.FIELD_MIN).asFloat();
            float maxBPM = dataPoint.getValue(Field.FIELD_MAX).asFloat();
            GFHRSummaryDataPoint hrSummary = new GFHRSummaryDataPoint(avgBPM, minBPM, maxBPM, start, dataSourceId);
            hrDataPoints.add(hrSummary);
        }
        return Tasks.forResult(hrDataPoints);
    }

    private GFHRDataPoint convertDataPointToHR(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_HEART_RATE_BPM.getFields()) {
            if (Field.FIELD_BPM.equals(field)) {
                float bpm = dataPoint.getValue(field).asFloat();
                return new GFHRDataPoint(bpm, start, dataSourceId);
            }
        }
        return null;
    }

    private GFPowerDataPoint convertDataPointToPower(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_POWER_SAMPLE.getFields()) {
            if (Field.FIELD_WATTS.equals(field)) {
                float watts = dataPoint.getValue(field).asFloat();
                return new GFPowerDataPoint(watts, start, dataSourceId);
            }
        }
        return null;
    }

    private GFSpeedDataPoint convertDataPointToSpeed(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_SPEED.getFields()) {
            if (Field.FIELD_SPEED.equals(field)) {
                float metersPerSecond = dataPoint.getValue(field).asFloat();
                return new GFSpeedDataPoint(metersPerSecond, start, dataSourceId);
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    private Task<List<GFSessionBundle>> bundleSessionsWithData(SessionReadResponse readResponse, Duration minSessionDuration, SupervisedExecutor gfTaskWatcher) {
        List<Session> sessions = readResponse.getSessions();
        Stream<Session> completedValuableSessions = sessions.stream()
            .filter(session -> {
                if (session.isOngoing()) {
                    return false;
                }
                long sessionStartAtMS = session.getStartTime(TimeUnit.MILLISECONDS);
                long sessionEndAtMS = session.getEndTime(TimeUnit.MILLISECONDS);
                Duration sessionDuration = Duration.ofMillis(sessionEndAtMS - sessionStartAtMS);
                return sessionDuration.compareTo(minSessionDuration) >= 0;
            });
        List<Task<GFSessionBundle>> sessionBundlesTasks = completedValuableSessions.map(session -> {
            TaskCompletionSource<GFSessionBundle> taskCompletionSource = new TaskCompletionSource<>(gfTaskWatcher.getCancellationToken());
            Supplier<Task<SessionReadResponse>> readSessionTaskSupplier = () -> {
                SessionReadRequest detailedSessionReadRequest = buildDetailedSessionReadRequest(session);
                return sessionsClient.readSession(detailedSessionReadRequest);
            };
            Task<SessionReadResponse> readSessionUnderWatch = runGFTaskUnderWatch(readSessionTaskSupplier, gfTaskWatcher);
            readSessionUnderWatch.addOnCompleteListener(executor, (commonResult) -> {
                if (!commonResult.isSuccessful() || commonResult.isCanceled()) {
                    if (readSessionUnderWatch.getException() != null) {
                        taskCompletionSource.trySetException(readSessionUnderWatch.getException());
                    }
                    return;
                }
                try {
                    GFSessionBundle gfSessionBundle = collectSessionBundleFromSessionResponse(readSessionUnderWatch.getResult());
                    taskCompletionSource.trySetResult(gfSessionBundle);
                } catch (Exception exc) {
                    taskCompletionSource.trySetException(exc);
                }
            });
            return taskCompletionSource.getTask();
        }).collect(Collectors.toList());

        Task<List<GFSessionBundle>> completedSessionBundlesTasks = Tasks.whenAll(sessionBundlesTasks).continueWithTask(executor, (commonResultTask) -> {
            if (commonResultTask.isCanceled() || !commonResultTask.isSuccessful()) {
                return Tasks.forException(GoogleTaskUtils.extractGFExceptionFromTasks(sessionBundlesTasks).orElse(commonResultTask.getException()));
            }
            List<GFSessionBundle> bundleList = sessionBundlesTasks.stream().map(t -> t.getResult()).collect(Collectors.toList());
            return Tasks.forResult(bundleList);
        });
        return completedSessionBundlesTasks;
    }

    @SuppressLint("NewApi")
    private GFSessionBundle collectSessionBundleFromSessionResponse(SessionReadResponse response) throws CommonException {
        if (response.getSessions().isEmpty()) {
            throw new CommonException("No session in the read response");
        }
        Session session = response.getSessions().get(0);
        GFSessionBundle.Builder sessionBundleBuilder = new GFSessionBundle.Builder();
        sessionBundleBuilder.setId(session.getIdentifier());
        sessionBundleBuilder.setName(session.getName());
        sessionBundleBuilder.setApplicationIdentifier(session.getAppPackageName());
        sessionBundleBuilder.setTimeStart(new Date(session.getStartTime(TimeUnit.MILLISECONDS)));
        sessionBundleBuilder.setTimeEnd(new Date(session.getEndTime(TimeUnit.MILLISECONDS)));
        sessionBundleBuilder.setType(zzjr.zzo(session.getActivity()));
        sessionBundleBuilder.setActivityType(session.getActivity());

        for (DataSet dataSet : response.getDataSet(session)) {
            DataType dataType = dataSet.getDataType();
            if (dataType.equals(DataType.TYPE_HEART_RATE_BPM)) {
                List<GFHRDataPoint> hr = convertDataSetToPoints(dataSet, this::convertDataPointToHR);
                sessionBundleBuilder.setHeartRate(hr);
            } else if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                List<GFStepsDataPoint> steps = convertDataSetToPoints(dataSet, this::convertDataPointToSteps);
                sessionBundleBuilder.setSteps(steps);
            } else if (dataType.equals(DataType.TYPE_SPEED)) {
                List<GFSpeedDataPoint> speed = convertDataSetToPoints(dataSet, this::convertDataPointToSpeed);
                sessionBundleBuilder.setSpeed(speed);
            } else if (dataType.equals(DataType.TYPE_POWER_SAMPLE)) {
                List<GFPowerDataPoint> power = convertDataSetToPoints(dataSet, this::convertDataPointToPower);
                sessionBundleBuilder.setPower(power);
            } else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                List<GFCalorieDataPoint> calories = convertDataSetToPoints(dataSet, this::convertDataPointToCalorie);
                sessionBundleBuilder.setCalories(calories);
            }
        }
        return sessionBundleBuilder.build();
    }

    @SuppressLint("NewApi")
    private <T extends GFDataPoint> List<T> convertDataSetToPoints(DataSet dataSet, Function<DataPoint, T> mapper) {
        return dataSet.getDataPoints().stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Task<DataReadResponse> readCaloriesHistory(Date start, Date end) {
        DataReadRequest readRequest = buildCaloriesDataReadRequest(start, end);
        return historyClient.readData(readRequest);
    }

    private Task<DataReadResponse> readStepsHistory(Date start, Date end) {
        DataReadRequest readRequest = buildStepsDataReadRequest(start, end);
        return historyClient.readData(readRequest);
    }

    private Task<DataReadResponse> readHRHistory(Date start, Date end, Duration bucketDuration) {
        DataReadRequest readRequest = buildHRDataReadRequest(start, end, bucketDuration);
        return historyClient.readData(readRequest);
    }

    private Task<SessionReadResponse> readSessions(Date start, Date end) {
        SessionReadRequest readRequest = buildSessionListReadRequest(start, end);
        return sessionsClient.readSession(readRequest);
    }

    private DataReadRequest buildCaloriesDataReadRequest(Date start, Date end) {
        return new DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }

    private DataReadRequest buildStepsDataReadRequest(Date start, Date end) {
        final DataSource estimatedStepsDataSource = new DataSource.Builder()
            .setAppPackageName("com.google.android.gms")
            .setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build();
        return new DataReadRequest.Builder()
            .aggregate(estimatedStepsDataSource)
            .bucketByTime(15, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }

    @SuppressLint("NewApi")
    private DataReadRequest buildHRDataReadRequest(Date start, Date end, Duration bucketDuration) {
        return new DataReadRequest.Builder()
            .aggregate(DataType.TYPE_HEART_RATE_BPM)
            .bucketByTime((int)bucketDuration.toMillis(), TimeUnit.MILLISECONDS)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }

    private SessionReadRequest buildSessionListReadRequest(Date start, Date end) {
        return new SessionReadRequest.Builder()
            .setTimeInterval(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .readSessionsFromAllApps()
            .build();
    }

    private SessionReadRequest buildDetailedSessionReadRequest(Session session) {
        return new SessionReadRequest.Builder()
            .readSessionsFromAllApps()
            .setTimeInterval(session.getStartTime(TimeUnit.MILLISECONDS), session.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
            .read(DataType.TYPE_HEART_RATE_BPM)
            .read(DataType.TYPE_STEP_COUNT_DELTA)
            .read(DataType.TYPE_SPEED)
            .read(DataType.TYPE_POWER_SAMPLE)
            .read(DataType.TYPE_CALORIES_EXPENDED)
            .setSessionId(session.getIdentifier())
            .build();
    }

    private ExecutorService createGfTaskWatcherExecutor() {
        return Executors.newFixedThreadPool(GF_TASK_WATCHER_THREAD_POOL_SIZE);
    }
}
