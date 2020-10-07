package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.core.util.Pair;
import androidx.core.util.Supplier;

import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.errors.GoogleFitActivitySourceExceptions.MaxRetriesExceededException;
import com.google.android.gms.fitness.FitnessActivities;
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
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// NOTE: a package below it's not supposed to be used as public api but I didn't find another way to
// get integer based constant of ActivityType by the string presentation which is returned by default
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

        // TODO: adjust size of chunks (duration) for the best performance
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        List<Task<List<GFCalorieDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadCaloriesTask(dateRange, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        }).collect(Collectors.toList());

        Task<List<GFCalorieDataPoint>> getCaloriesTask = concludeWatchedTasksResults(tasks, gfTaskWatcherExecutor);
        return getCaloriesTask;
    }

    @SuppressLint("NewApi")
    public Task<List<GFStepsDataPoint>> getSteps(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofDays(12));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        List<Task<List<GFStepsDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadStepsTask(dateRange, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        }).collect(Collectors.toList());

        Task<List<GFStepsDataPoint>> getStepsTask = concludeWatchedTasksResults(tasks, gfTaskWatcherExecutor);
        return getStepsTask;
    }

    @SuppressLint("NewApi")
    public Task<List<GFHRDataPoint>> getHRs(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        List<Task<List<GFHRDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadHRTask(dateRange, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        }).collect(Collectors.toList());

        Task<List<GFHRDataPoint>> getHRTask = concludeWatchedTasksResults(tasks, gfTaskWatcherExecutor);
        return getHRTask;
    }

    @SuppressLint("NewApi")
    public Task<Void> getSessions(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();

//        List<Task<List<GFHRDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
//            return runReadHRTask(dateRange, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
//        }).collect(Collectors.toList());


        Task<SessionReadResponse> readSessionTask = sessionsClient.readSession(buildSessionsDataReadRequest(start, end));
        readSessionTask.continueWith(gfTaskWatcherExecutor, (task) -> {
            Log.d(TAG, "getSessions: session task success = " + task.isSuccessful());
            if (!task.isSuccessful()) {
                Log.d(TAG, "getSessions: session task exception = " + task.getException());
            }
            SessionReadResponse response = task.getResult();
            Log.d(TAG, "getSessions: sessions count: " + response.getSessions().size());
            List<Session> sessions = response.getSessions();
            for (Session session : sessions) {
                Date startSession = new Date(session.getStartTime(TimeUnit.MILLISECONDS));
                Date endSession = new Date(session.getEndTime(TimeUnit.MILLISECONDS));
//                session.
//                FitnessActivities.getMimeType()
//                session.e
//                session.getActivity()
                Log.d(TAG, "getSessions: " + String.format("id: %s, start: %s, end: %s, activity: %s, name: %s,",
                    session.getIdentifier(),
                    startSession,
                    endSession,
                    session.getActivity(),
                    session.getName()
                    ));

                Log.d(TAG, "getSessions: ACTIVITY_TYPE: " + zzjr.zzo(session.getActivity()));

//                try {
//                    DataReadRequest sessionCaloriedReadRequest = new DataReadRequest.Builder()
//                        .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
//                        .bucketByTime(1, TimeUnit.MINUTES)
//                        .setTimeRange(session.getStartTime(TimeUnit.MILLISECONDS), session.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
//                        .build();
//                    historyClient.readData(sessionCaloriedReadRequest);
//                } catch (Throwable t) {
//                    t.printStackTrace();
//                }

                List<DataSet> dataSets = response.getDataSet(session);
                for (DataSet dataSet : dataSets) {
                    if (dataSet.isEmpty()) {
                        continue;
                    }
                    if (dataSet.getDataType().equals(DataType.TYPE_CALORIES_EXPENDED)) {
                        for (DataPoint dataPoint : dataSet.getDataPoints()) {
                            GFCalorieDataPoint caloroie = convertDataPointToCalorie(dataPoint);
                            Log.d(TAG, "getSessions: CALORIE " + caloroie);
                        }
                    }
                    if (dataSet.getDataType().equals(DataType.TYPE_HEART_RATE_BPM)) {
                        for (DataPoint dataPoint : dataSet.getDataPoints()) {
                            GFHRDataPoint hr = convertDataPointToHR(dataPoint);
                            Log.d(TAG, "getSessions: HR" + hr);
                        }
                    }
//                    if (dataSet.getDataType().equals(DataType.TYPE_ACTIVITY_SEGMENT));
                }

            }
            return null;
        });
        return null;
    }

    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Pair<Date, Date> dateRange, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        Supplier<Task<List<GFCalorieDataPoint>>> taskSupplier = () -> {
            return readCaloriesHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, this::convertDataReadResponseToCalories);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
    }

    private Task<List<GFStepsDataPoint>> runReadStepsTask(Pair<Date, Date> dateRange, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        Supplier<Task<List<GFStepsDataPoint>>> taskSupplier = () -> {
            return readStepsHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, this::convertToSteps);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
    }

    private Task<List<GFHRDataPoint>> runReadHRTask(Pair<Date, Date> dateRange, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        Supplier<Task<List<GFHRDataPoint>>> taskSupplier = () -> {
            return readHRHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, this::convertToHR);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
    }

//    private Task<List<Object>> runReadSessions(Pair<Date, Date> dateRange, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
//
//    }

    private <T> Task<T> runGFTaskUnderWatch(Supplier<Task<T>> taskSupplier, ExecutorService gfTaskWatcherExecutor, CancellationTokenSource cancellationTokenSource, CancellationToken cancellationToken) {
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(cancellationToken);
        gfTaskWatcherExecutor.execute(() -> {
            if (cancellationToken.isCancellationRequested()) {
                return;
            }
            for (int tryNumber = 1; tryNumber <= RETRIES_COUNT && !cancellationToken.isCancellationRequested(); tryNumber++) {
                Log.d(TAG, String.format("runGFTaskUnderWatch: awaiting #%d", tryNumber));
                try {
                    Task<T> originalTask = taskSupplier.get();
                    T result = Tasks.await(originalTask, GF_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    Log.d(TAG, String.format("runGFTaskUnderWatch: completed #%d", tryNumber));
                    taskCompletionSource.trySetResult(result);
                    return;
                } catch (
                    // TODO: catch unauthorized exception
                    java.util.concurrent.ExecutionException | InterruptedException | java.util.concurrent.TimeoutException exc) {
                    Log.d(TAG, String.format("runReadCaloriesTask: failed #%d", tryNumber));
                    continue;
                }
            }
            if (cancellationToken.isCancellationRequested()) {
                taskCompletionSource.trySetException(new GoogleFitActivitySourceExceptions.CommonException("One of the previous tasks was canceled"));
            } else {
//                Log.d(TAG, "runGFTaskUnderWatch: Too many retries");
                // TODO: introduce new class for gf task parameters
                taskCompletionSource.trySetException(new MaxRetriesExceededException("Too many retries"));
                cancellationTokenSource.cancel();
            }
        });
        return taskCompletionSource.getTask();
    }

    @SuppressLint("NewApi")
    private <V, T extends List<V>> Task<T> concludeWatchedTasksResults(List<Task<T>> tasks, ExecutorService gfTaskWatcherExecutor) {
        Task<T> collectResultsTask = Tasks.whenAll(tasks).continueWithTask(executor, commonResult -> {
            if (commonResult.isCanceled()) {
                return Tasks.forException(new Exception("Pooling task was canceled"));
            } else if (commonResult.getException() != null) {
                Optional<Task<T>> failedTaskOpt = tasks.stream()
                    .filter(t -> t.getException() != null && t.getException() instanceof GoogleFitActivitySourceExceptions.CommonException)
                    .findFirst();
                Exception exception = failedTaskOpt.map(Task::getException).orElse(commonResult.getException());
                return Tasks.forException(exception);
            }
            T flattenResults = (T) tasks.stream()
                .flatMap(t -> t.getResult().stream())
                .collect(Collectors.toList());
            return Tasks.forResult(flattenResults);
        });
        Task<T> taskWithShutdown = collectResultsTask.addOnCompleteListener(executor, (task) -> {
            // Log.d(TAG, "concludeWatchedTasksResults: shutdown!");
            // TODO: check if it was really shutdown after a while
            gfTaskWatcherExecutor.shutdownNow();
        });
        return taskWithShutdown;
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
        for (Field field : DataType.TYPE_CALORIES_EXPENDED.getFields()) {
            if (Field.FIELD_CALORIES.equals(field)) {
                float kcals = dataPoint.getValue(field).asFloat();
                GFCalorieDataPoint calorie = new GFCalorieDataPoint(kcals, start, dataSourceId);
                return calorie;
            }
        }
        return null;
    }

    private Task<List<GFStepsDataPoint>> convertToSteps(DataReadResponse dataReadResponse) {
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

    private Task<List<GFHRDataPoint>> convertToHR(DataReadResponse dataReadResponse) {
        ArrayList<GFHRDataPoint> hrDataPoints = new ArrayList<>();
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
            for (Field field : DataType.TYPE_HEART_RATE_BPM.getAggregateType().getFields()) {
                if (Field.FIELD_AVERAGE.equals(field)) {
                    float avgBPM = dataPoint.getValue(field).asFloat();
                    GFHRDataPoint hrDataPoint = new GFHRDataPoint(avgBPM, start, dataSourceId);
                    hrDataPoints.add(hrDataPoint);
                }
            }
        }
        return Tasks.forResult(hrDataPoints);
    }

    private GFHRDataPoint convertDataPointToHR(DataPoint dataPoint) {
        String dataSourceId = dataPoint.getOriginalDataSource().getStreamIdentifier();
        Date start = new Date(dataPoint.getStartTime(TimeUnit.MILLISECONDS));
        for (Field field : DataType.TYPE_HEART_RATE_BPM.getFields()) {
            if (Field.FIELD_BPM.equals(field)) {
                float bpm = dataPoint.getValue(field).asFloat();
                GFHRDataPoint hrDataPoint = new GFHRDataPoint(bpm, start, dataSourceId);
                return hrDataPoint;
            }
        }
        return null;
    }

    private Task<DataReadResponse> readCaloriesHistory(Date start, Date end) {
        DataReadRequest readRequest = buildCaloriesDataReadRequest(start, end);
        return historyClient.readData(readRequest);
    }

    private Task<DataReadResponse> readStepsHistory(Date start, Date end) {
        DataReadRequest readRequest = buildStepsDataReadRequest(start, end);
        return historyClient.readData(readRequest);
    }

    private Task<DataReadResponse> readHRHistory(Date start, Date end) {
        DataReadRequest readRequest = buildHRDataReadRequest(start, end);
        return historyClient.readData(readRequest);

    }

    private Task<SessionReadResponse> readSessions(Date start, Date end) {
        SessionReadRequest readRequest = buildSessionsDataReadRequest(start, end);
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

    private DataReadRequest buildHRDataReadRequest(Date start, Date end) {
        return new DataReadRequest.Builder()
            .aggregate(DataType.TYPE_HEART_RATE_BPM)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }

    private SessionReadRequest buildSessionsDataReadRequest(Date start, Date end) {
        return new SessionReadRequest.Builder()
            .setTimeInterval(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .readSessionsFromAllApps()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .read(DataType.TYPE_STEP_COUNT_DELTA)
            .read(DataType.TYPE_SPEED)
            .build();
    }

    private ExecutorService createGfTaskWatcherExecutor() {
        return Executors.newFixedThreadPool(GF_TASK_WATCHER_THREAD_POOL_SIZE);
    }
}
