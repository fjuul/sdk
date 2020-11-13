package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import androidx.core.util.Pair;
import androidx.core.util.Supplier;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxRetriesExceededException;
import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.shutdownExecutorsOnComplete;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
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

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private static final int RETRIES_COUNT = 3;
    private static final int GF_TASK_WATCHER_THREAD_POOL_SIZE = 1;
    private static final long GF_QUERY_TIMEOUT_SECONDS = 60l;
    private static final long GF_DETAILED_SESSION_QUERY_TIMEOUT_SECONDS = 60L;

    private final HistoryClient historyClient;
    private final SessionsClient sessionsClient;
    private final GFDataUtils gfUtils;
    // TODO: rename executor
    private final Executor executor;
    private final SimpleDateFormat dateFormatter;

    public GFClientWrapper(HistoryClient historyClient, SessionsClient sessionsClient, GFDataUtils gfUtils) {
        this.historyClient = historyClient;
        this.sessionsClient = sessionsClient;
        this.gfUtils = gfUtils;
        // TODO: choose the best executor for this wrapper needs (gf data converting)
        executor = Executors.newCachedThreadPool();
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
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
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofDays(7));

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
            return runReadHRTask(dateRange, gfTaskWatcherDetails);
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

    @SuppressLint("NewApi")
    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
         final Supplier<SupervisedTask<List<GFCalorieDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFCalorieDataPoint>> convertData = response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToCalorie);
            Task<List<GFCalorieDataPoint>> task = readAggregatedCaloriesHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, convertData.andThen(Tasks::forResult)::apply);
             return buildSupervisedTask("fetch gf intraday calories", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    @SuppressLint("NewApi")
    private Task<List<GFStepsDataPoint>> runReadStepsTask(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
         final Supplier<SupervisedTask<List<GFStepsDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFStepsDataPoint>> convertData = response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToSteps);
            final Task<List<GFStepsDataPoint>> task = readAggregatedStepsHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, convertData.andThen(Tasks::forResult)::apply);
            return buildSupervisedTask("fetch gf intraday steps", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    @SuppressLint("NewApi")
    private Task<List<GFHRSummaryDataPoint>> runReadHRTask(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
        Supplier<SupervisedTask<List<GFHRSummaryDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFHRSummaryDataPoint>> convertData = response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToHRSummary);
            Task<List<GFHRSummaryDataPoint>> task = readAggregatedHRHistory(dateRange.first, dateRange.second)
                .onSuccessTask(executor, convertData.andThen(Tasks::forResult)::apply);
            return buildSupervisedTask("fetch gf intraday hr", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    private Task<List<GFSessionBundle>> runReadSessions(Pair<Date, Date> dateRange, Duration minSessionDuration, SupervisedExecutor gfTaskWatcher, SupervisedExecutor gfSubTaskWatcher) {
        Supplier<SupervisedTask<SessionReadResponse>> taskSupplier = () -> {
            Task<SessionReadResponse> task = readSessions(dateRange.first, dateRange.second);
            return buildSupervisedTask("fetch gf sessions", task, dateRange);
        };
        Task<SessionReadResponse> readRawSessionsUnderWatchTask = runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
        Task<List<GFSessionBundle>> bundledSessionsTask = readRawSessionsUnderWatchTask.onSuccessTask(gfTaskWatcher.getExecutor(), (readResponse) -> {
            return bundleSessionsWithData(readResponse, minSessionDuration, gfSubTaskWatcher);
        });
        return bundledSessionsTask;
    }

    private <T> Task<T> runGFTaskUnderWatch(Supplier<SupervisedTask<T>> taskSupplier, SupervisedExecutor gfTaskWatcher) {
        ExecutorService gfTaskWatcherExecutor = gfTaskWatcher.getExecutor();
        TaskCompletionSource<T> taskCompletionSource = new TaskCompletionSource<>(gfTaskWatcher.getCancellationToken());
        CancellationTokenSource cancellationTokenSource = gfTaskWatcher.getCancellationTokenSource();
        CancellationToken cancellationToken = gfTaskWatcher.getCancellationToken();
        gfTaskWatcherExecutor.execute(() -> {
            final SupervisedTask<T> supervisedTask = taskSupplier.get();
            for (int tryNumber = 1; tryNumber <= supervisedTask.getRetriesCount() && !cancellationToken.isCancellationRequested(); tryNumber++) {
                try {
                    Task<T> originalTask = supervisedTask.getTask();
                    T result = Tasks.await(originalTask, supervisedTask.getTimeoutSeconds(), TimeUnit.SECONDS);
                    taskCompletionSource.trySetResult(result);
                    return;
                } catch (InterruptedException | TimeoutException exc) {
                    // expected exception due to either the task cancellation or timeout, give a try again
                    continue;
                } catch (ExecutionException exc) {
                    // exception originated from the completed task
                    Exception exception = new GoogleFitActivitySourceExceptions.CommonException(exc.getCause());
                    taskCompletionSource.trySetException(exception);
                    return;
                }
            }
            if (cancellationToken.isCancellationRequested()) {
                return;
            }
            String exceptionMessage = String.format("Possible retries count (%d) exceeded for task \"%s\"",
                supervisedTask.getRetriesCount(),
                supervisedTask.getName());
            taskCompletionSource.trySetException(new MaxRetriesExceededException(exceptionMessage));
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
            Supplier<SupervisedTask<SessionReadResponse>> readSessionTaskSupplier = () -> {
                SessionReadRequest detailedSessionReadRequest = buildDetailedSessionReadRequest(session);
                Task<SessionReadResponse> task = sessionsClient.readSession(detailedSessionReadRequest);
                String taskName = String.format("fetch detailed gf session %s", session.getIdentifier());
                return new SupervisedTask<>(taskName, task, GF_DETAILED_SESSION_QUERY_TIMEOUT_SECONDS, RETRIES_COUNT);
            };
            Task<SessionReadResponse> readSessionUnderWatch = runGFTaskUnderWatch(readSessionTaskSupplier, gfTaskWatcher);
            Task<GFSessionBundle> collectSessionBundleTask = readSessionUnderWatch.onSuccessTask(executor, sessionReadResponse -> {
                GFSessionBundle gfSessionBundle = collectSessionBundleFromSessionResponse(readSessionUnderWatch.getResult());
                return Tasks.forResult(gfSessionBundle);
            });
            // NOTE: It is possible that GF Session and its samples are too big for passing it through IPC Binder (TransactionTooLarge),
            // for example: android.os.TransactionTooLargeException: Error while delivering result of client request SessionReadRequest.
            // For this rare case, we have to try to fetch all related data points one by one.
            Task<GFSessionBundle> rescueSessionBundleTask = collectSessionBundleTask.continueWithTask(executor, (task) -> {
                if (task.getException() instanceof MaxRetriesExceededException) {
                    return collectSessionBundleWithDataReadRequests(session, gfTaskWatcher);
                }
                return task;
            });

            rescueSessionBundleTask.addOnCompleteListener(executor, (commonResultTask) -> {
                if (!commonResultTask.isSuccessful() || commonResultTask.isCanceled()) {
                    taskCompletionSource.trySetException(commonResultTask.getException());
                    return;
                }
                taskCompletionSource.trySetResult(commonResultTask.getResult());
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
        GFSessionBundle.Builder sessionBundleBuilder = initSessionBundleBuilderFromSession(session);
        for (DataSet dataSet : response.getDataSet(session)) {
            DataType dataType = dataSet.getDataType();
            if (dataType.equals(DataType.TYPE_HEART_RATE_BPM)) {
                List<GFHRDataPoint> hr = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToHR);
                sessionBundleBuilder.setHeartRate(hr);
            } else if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                List<GFStepsDataPoint> steps = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToSteps);
                sessionBundleBuilder.setSteps(steps);
            } else if (dataType.equals(DataType.TYPE_SPEED)) {
                List<GFSpeedDataPoint> speed = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToSpeed);
                sessionBundleBuilder.setSpeed(speed);
            } else if (dataType.equals(DataType.TYPE_POWER_SAMPLE)) {
                List<GFPowerDataPoint> power = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToPower);
                sessionBundleBuilder.setPower(power);
            } else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                List<GFCalorieDataPoint> calories = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToCalorie);
                sessionBundleBuilder.setCalories(calories);
            } else if (dataType.equals(DataType.TYPE_ACTIVITY_SEGMENT)) {
                List<GFActivitySegmentDataPoint> activitySegments = convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToActivitySegment);
                sessionBundleBuilder.setActivitySegments(activitySegments);
            }
        }
        return sessionBundleBuilder.build();
    }

    @SuppressLint("NewApi")
    private Task<GFSessionBundle> collectSessionBundleWithDataReadRequests(Session session, SupervisedExecutor gfTaskWatcher) {
        Date sessionStart = new Date(session.getStartTime(TimeUnit.MILLISECONDS));
        Date sessionEnd = new Date(session.getEndTime(TimeUnit.MILLISECONDS));
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(sessionStart, sessionEnd, Duration.ofMinutes(15));
        List<DataReadRequest> readHrRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_HEART_RATE_BPM);
        List<DataReadRequest> readStepsRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_STEP_COUNT_DELTA);
        List<DataReadRequest> readSpeedRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_SPEED);
        List<DataReadRequest> readPowerRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_POWER_SAMPLE);
        List<DataReadRequest> readCaloriesRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_CALORIES_EXPENDED);
        List<DataReadRequest> readSegmentsRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_ACTIVITY_SEGMENT);

        Task<List<GFHRDataPoint>> getSessionHRTask = runAndConcatSoleDataReadRequests(
            readHrRequests,
            GFDataConverter::convertDataPointToHR,
            (req) -> String.format("fetch gf HR for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFStepsDataPoint>> getSessionStepsTask = runAndConcatSoleDataReadRequests(
            readStepsRequests,
            GFDataConverter::convertDataPointToSteps,
            (req) -> String.format("fetch gf steps for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFSpeedDataPoint>> getSessionSpeedTask = runAndConcatSoleDataReadRequests(
            readSpeedRequests,
            GFDataConverter::convertDataPointToSpeed,
            (req) -> String.format("fetch gf speed for session %s", session.getIdentifier()),
            gfTaskWatcher
        );
        Task<List<GFPowerDataPoint>> getSessionPowerTask = runAndConcatSoleDataReadRequests(
            readPowerRequests,
            GFDataConverter::convertDataPointToPower,
            (req) -> String.format("fetch gf power for session %s", session.getIdentifier()),
            gfTaskWatcher
        );
        Task<List<GFCalorieDataPoint>> getSessionCaloriesTask = runAndConcatSoleDataReadRequests(
            readCaloriesRequests,
            GFDataConverter::convertDataPointToCalorie,
            (req) -> String.format("fetch gf calories for session %s", session.getIdentifier()),
            gfTaskWatcher
        );
        Task<List<GFActivitySegmentDataPoint>> getSessionSegmentsTask = runAndConcatSoleDataReadRequests(
            readSegmentsRequests,
            GFDataConverter::convertDataPointToActivitySegment,
            (req) -> String.format("fetch gf activity segments for session %s", session.getIdentifier()),
            gfTaskWatcher
        );

        List<Task<?>> tasks = Arrays.asList(
            getSessionHRTask,
            getSessionStepsTask,
            getSessionSpeedTask,
            getSessionPowerTask,
            getSessionCaloriesTask,
            getSessionSegmentsTask
        );
        Task<GFSessionBundle> collectSessionBundleTask = Tasks.whenAll(tasks).continueWithTask(executor, (commonResultTask) -> {
            if (commonResultTask.isCanceled() || !commonResultTask.isSuccessful()) {
                return Tasks.forException(GoogleTaskUtils.extractGFExceptionFromTasks(tasks).orElse(commonResultTask.getException()));
            }
            GFSessionBundle.Builder sessionBundleBuilder = initSessionBundleBuilderFromSession(session);
            sessionBundleBuilder.setHeartRate(getSessionHRTask.getResult());
            sessionBundleBuilder.setSteps(getSessionStepsTask.getResult());
            sessionBundleBuilder.setSpeed(getSessionSpeedTask.getResult());
            sessionBundleBuilder.setPower(getSessionPowerTask.getResult());
            sessionBundleBuilder.setCalories(getSessionCaloriesTask.getResult());
            sessionBundleBuilder.setActivitySegments(getSessionSegmentsTask.getResult());
            return Tasks.forResult(sessionBundleBuilder.build());
        });
        return collectSessionBundleTask;
    }

    @SuppressLint("NewApi")
    private List<DataReadRequest> buildChunkedDataTypeReadRequests(List<Pair<Date, Date>> dateChunks, DataType type) {
        return dateChunks.stream()
            .map(dateRange -> buildDataTypeReadRequest(type, dateRange.first, dateRange.second))
            .collect(Collectors.toList());
    }

    @SuppressLint("NewApi")
    private <T extends GFDataPoint> Task<List<T>> runAndConcatSoleDataReadRequests(
        List<DataReadRequest> readRequests,
        Function<DataPoint, T> dataPointMapper,
        Function<DataReadRequest, String> taskNameGenerator,
        SupervisedExecutor gfTaskWatcher) {
        List<Task<List<T>>> tasks = readRequests.stream().map(request -> {
            final Supplier<SupervisedTask<List<T>>> supplier = () -> {
                final Function<DataReadResponse, List<T>> convertData = response -> {
                    final DataSet dataSet = response.getDataSet(request.getDataTypes().get(0));
                    return convertDataSetToPoints(dataSet, dataPointMapper);
                };
                final Task<List<T>> task = historyClient.readData(request)
                    .onSuccessTask(executor, convertData.andThen(Tasks::forResult)::apply);
                Date start = new Date(request.getStartTime(TimeUnit.MILLISECONDS));
                Date end = new Date(request.getEndTime(TimeUnit.MILLISECONDS));
                String jobName = taskNameGenerator.apply(request);
                return buildSupervisedTask(jobName, task, Pair.create(start, end));
            };
            return runGFTaskUnderWatch(supplier, gfTaskWatcher);
        }).collect(Collectors.toList());
        return flatMapTasksResults(tasks);
    }

    private static GFSessionBundle.Builder initSessionBundleBuilderFromSession(Session session) {
        GFSessionBundle.Builder sessionBundleBuilder = new GFSessionBundle.Builder();
        sessionBundleBuilder.setId(session.getIdentifier());
        sessionBundleBuilder.setName(session.getName());
        sessionBundleBuilder.setApplicationIdentifier(session.getAppPackageName());
        sessionBundleBuilder.setTimeStart(new Date(session.getStartTime(TimeUnit.MILLISECONDS)));
        sessionBundleBuilder.setTimeEnd(new Date(session.getEndTime(TimeUnit.MILLISECONDS)));
        sessionBundleBuilder.setType(zzjr.zzo(session.getActivity()));
        sessionBundleBuilder.setActivityType(session.getActivity());
        return sessionBundleBuilder;
    }

    @SuppressLint("NewApi")
    private static <T extends GFDataPoint> List<T> convertDataSetToPoints(DataSet dataSet, Function<DataPoint, T> mapper) {
        return dataSet.getDataPoints().stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @SuppressLint("NewApi")
    private static <T extends GFDataPoint> List<T> convertResponseBucketsToPoints(DataReadResponse response, Function<Bucket, T> mapper) {
        return response.getBuckets().stream()
            .map(mapper)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private <T> SupervisedTask<T> buildSupervisedTask(String jobName, Task<T> task, Pair<Date, Date> dateRange) {
        Date start = dateRange.first;
        Date end = dateRange.second;
        String taskName = String.format("'%s' for %s-%s", jobName, dateFormatter.format(start), dateFormatter.format(end));
        return new SupervisedTask<>(taskName, task, GF_QUERY_TIMEOUT_SECONDS, RETRIES_COUNT);
    }

    private Task<DataReadResponse> readAggregatedCaloriesHistory(Date start, Date end) {
        DataReadRequest request = new DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<DataReadResponse> readAggregatedStepsHistory(Date start, Date end) {
        final DataSource estimatedStepsDataSource = new DataSource.Builder()
            .setAppPackageName("com.google.android.gms")
            .setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build();
        DataReadRequest request = new DataReadRequest.Builder()
            .aggregate(estimatedStepsDataSource)
            .bucketByTime(15, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<DataReadResponse> readAggregatedHRHistory(Date start, Date end) {
        DataReadRequest request = new DataReadRequest.Builder()
            .aggregate(DataType.TYPE_HEART_RATE_BPM)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<SessionReadResponse> readSessions(Date start, Date end) {
        SessionReadRequest readRequest = buildSessionListReadRequest(start, end);
        return sessionsClient.readSession(readRequest);
    }

    private DataReadRequest buildDataTypeReadRequest(DataType type, Date start, Date end) {
        return new DataReadRequest.Builder()
            .read(type)
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
            .read(DataType.TYPE_ACTIVITY_SEGMENT)
            .setSessionId(session.getIdentifier())
            .build();
    }

    private ExecutorService createGfTaskWatcherExecutor() {
        return Executors.newFixedThreadPool(GF_TASK_WATCHER_THREAD_POOL_SIZE);
    }
}
