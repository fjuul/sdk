package com.fjuul.sdk.activitysources.entities.internal;

import static com.fjuul.sdk.activitysources.utils.GoogleTaskUtils.shutdownExecutorsOnComplete;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFActivitySegmentDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataConverter;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFPowerDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSpeedDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFWeightDataPoint;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException;
import com.fjuul.sdk.activitysources.utils.GoogleTaskUtils;
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
import com.google.android.gms.internal.fitness.zzfv;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.core.util.Supplier;

// NOTE#1: the package com.google.android.gms.internal.fitness.zzfv it's not supposed to be used as public api but I
// didn't find another way to get the integer based constant of ActivityType by the string presentation which is
// returned by default along with a session.
// Don't worry if it wasn't discovered after the upgrade of `play-services-fitness`, it may be placed under a different
// name.

// NOTE#2: GF can silently fail on a request if response data is too large, more details at
// https://stackoverflow.com/a/55806509/6685359.
// Also there may be a case when GoogleFit service can't response to the requester on the first attempts due to failed
// delivery.
// Therefore the wrapper divide one big request into smaller ones and use a fixed thread pool to watch for the fired
// requests with a timeout and retries.

// NOTE#3: requesting intraday data from Google Fit, we split the input date range into days in the local timezone,
// since a day is a minimum, atomic unit in terms of the syncing interval. Even if it's more effective to query data by
// a bigger date range in some cases (for example, steps), do not change the size of the splitting while GF
// synchronization interval remains input for SDK consumers.

class GFClientWrapper {
    private static final String TAG = "GFClientWrapper";

    static class Config {
        final int queryRetriesCount;
        final long queryTimeoutSeconds;
        final int detailedSessionQueryRetriesCount;
        final long detailedSessionQueryTimeoutSeconds;

        public Config(int queryRetriesCount,
            long queryTimeoutSeconds,
            int detailedSessionQueryRetriesCount,
            long detailedSessionQueryTimeoutSeconds) {
            this.queryRetriesCount = queryRetriesCount;
            this.queryTimeoutSeconds = queryTimeoutSeconds;
            this.detailedSessionQueryRetriesCount = detailedSessionQueryRetriesCount;
            this.detailedSessionQueryTimeoutSeconds = detailedSessionQueryTimeoutSeconds;
        }
    }

    static final Config DEFAULT_CONFIG = new Config(0, 60, 0, 40);

    private final Config config;
    private final HistoryClient historyClient;
    private final SessionsClient sessionsClient;
    private final GFDataUtils gfUtils;
    private final Executor localBackgroundExecutor;
    private final ThreadLocal<SimpleDateFormat> dateFormatter = new ThreadLocal<SimpleDateFormat>() {
        @Nullable
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        }
    };

    public GFClientWrapper(HistoryClient historyClient, SessionsClient sessionsClient, GFDataUtils gfUtils) {
        this(DEFAULT_CONFIG, historyClient, sessionsClient, gfUtils);
    }

    GFClientWrapper(Config config, HistoryClient historyClient, SessionsClient sessionsClient, GFDataUtils gfUtils) {
        this.historyClient = historyClient;
        this.sessionsClient = sessionsClient;
        this.gfUtils = gfUtils;
        this.config = config;
        this.localBackgroundExecutor = Executors.newCachedThreadPool();
    }

    @SuppressLint("NewApi")
    public Task<List<GFCalorieDataPoint>> getCalories(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        // see NOTE#3 at the top
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoDays(start, end);

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFCalorieDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadCaloriesTask(dateRange, gfTaskWatcher);
        }).collect(Collectors.toList());

        Task<List<GFCalorieDataPoint>> getCaloriesTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getCaloriesTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<List<GFStepsDataPoint>> getSteps(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        // see NOTE#3 at the top
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoDays(start, end);

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFStepsDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadStepsTask(dateRange, gfTaskWatcher);
        }).collect(Collectors.toList());

        Task<List<GFStepsDataPoint>> getStepsTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getStepsTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<List<GFHRSummaryDataPoint>> getHRSummaries(Date start, Date end) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        // see NOTE#3 at the top
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoDays(start, end);

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        SupervisedExecutor gfTaskWatcherDetails =
            new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);
        List<Task<List<GFHRSummaryDataPoint>>> tasks = dateChunks.stream().map(dateRange -> {
            return runReadHRTask(dateRange, gfTaskWatcherDetails);
        }).collect(Collectors.toList());

        Task<List<GFHRSummaryDataPoint>> getHRTask = flatMapTasksResults(tasks);
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getHRTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<List<GFSessionBundle>> getSessions(Date start, Date end, Duration minSessionDuration) {
        ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        CancellationToken cancellationToken = cancellationTokenSource.getToken();
        SupervisedExecutor gfReadSessionsWatcher =
            new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource, cancellationToken);
        // NOTE: here we don't split the input date range into days because workouts that start before midnight and
        // finish after midnight wouldn't be found in this case
        List<Pair<Date, Date>> dateChunks = gfUtils.splitDateRangeIntoChunks(start, end, Duration.ofDays(5));

        List<Task<List<Session>>> readSessionListTasks = dateChunks.stream().map(dateRange -> {
            return runReadRawSessionList(dateRange, gfReadSessionsWatcher);
        }).collect(Collectors.toList());
        Task<List<Session>> rawSessionListTask = flatMapTasksResults(readSessionListTasks);

        Task<List<Session>> filteredSessionListTask =
            rawSessionListTask.onSuccessTask(localBackgroundExecutor, (sessions) -> {
                Stream<Session> completedValuableSessions = sessions.stream().filter(session -> {
                    if (session.isOngoing()) {
                        return false;
                    }
                    long sessionStartAtMS = session.getStartTime(TimeUnit.MILLISECONDS);
                    long sessionEndAtMS = session.getEndTime(TimeUnit.MILLISECONDS);
                    Duration sessionDuration = Duration.ofMillis(sessionEndAtMS - sessionStartAtMS);
                    return sessionDuration.compareTo(minSessionDuration) >= 0;
                });
                return Tasks.forResult(completedValuableSessions.collect(Collectors.toList()));
            });

        Task<List<GFSessionBundle>> getSessionsTask =
            filteredSessionListTask.onSuccessTask(localBackgroundExecutor, (sessions -> {
                return runReadDetailedSessionBundles(sessions, gfReadSessionsWatcher);
            }));
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getSessionsTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<GFHeightDataPoint> getLastKnownHeight() {
        final ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);

        final Supplier<SupervisedTask<GFHeightDataPoint>> taskSupplier = () -> {
            final Function<DataReadResponse, GFHeightDataPoint> convertData =
                response -> convertResponseDataSetToPoint(response,
                    DataType.TYPE_HEIGHT,
                    GFDataConverter::convertDataPointToHeight);
            final Task<GFHeightDataPoint> task = readLastKnownDataPointOfType(DataType.TYPE_HEIGHT)
                .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
            return new SupervisedTask<>("fetch gf user's height",
                task,
                config.queryTimeoutSeconds,
                config.queryRetriesCount);
        };
        final Task<GFHeightDataPoint> getHeightTask = runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getHeightTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    public Task<GFWeightDataPoint> getLastKnownWeight() {
        final ExecutorService gfTaskWatcherExecutor = createGfTaskWatcherExecutor();
        final CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        final SupervisedExecutor gfTaskWatcher = new SupervisedExecutor(gfTaskWatcherExecutor, cancellationTokenSource);

        final Supplier<SupervisedTask<GFWeightDataPoint>> taskSupplier = () -> {
            final Function<DataReadResponse, GFWeightDataPoint> convertData =
                response -> convertResponseDataSetToPoint(response,
                    DataType.TYPE_WEIGHT,
                    GFDataConverter::convertDataPointToWeight);
            final Task<GFWeightDataPoint> task = readLastKnownDataPointOfType(DataType.TYPE_WEIGHT)
                .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
            return new SupervisedTask<>("fetch gf user's weight",
                task,
                config.queryTimeoutSeconds,
                config.queryRetriesCount);
        };
        final Task<GFWeightDataPoint> getWeightTask = runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
        return shutdownExecutorsOnComplete(localBackgroundExecutor, getWeightTask, gfTaskWatcherExecutor);
    }

    @SuppressLint("NewApi")
    private Task<List<GFCalorieDataPoint>> runReadCaloriesTask(Pair<Date, Date> dateRange,
        SupervisedExecutor gfTaskWatcher) {
        final Supplier<SupervisedTask<List<GFCalorieDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFCalorieDataPoint>> convertData =
                response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToCalorie);
            Task<List<GFCalorieDataPoint>> task = readAggregatedCaloriesHistory(dateRange.first, dateRange.second)
                .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
            return buildSupervisedTask("fetch gf intraday calories", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    @SuppressLint("NewApi")
    private Task<List<GFStepsDataPoint>> runReadStepsTask(Pair<Date, Date> dateRange,
        SupervisedExecutor gfTaskWatcher) {
        final Supplier<SupervisedTask<List<GFStepsDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFStepsDataPoint>> convertData =
                response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToSteps);
            final Task<List<GFStepsDataPoint>> task = readAggregatedStepsHistory(dateRange.first, dateRange.second)
                .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
            return buildSupervisedTask("fetch gf intraday steps", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    @SuppressLint("NewApi")
    private Task<List<GFHRSummaryDataPoint>> runReadHRTask(Pair<Date, Date> dateRange,
        SupervisedExecutor gfTaskWatcher) {
        Supplier<SupervisedTask<List<GFHRSummaryDataPoint>>> taskSupplier = () -> {
            final Function<DataReadResponse, List<GFHRSummaryDataPoint>> convertData =
                response -> convertResponseBucketsToPoints(response, GFDataConverter::convertBucketToHRSummary);
            Task<List<GFHRSummaryDataPoint>> task = readAggregatedHRHistory(dateRange.first, dateRange.second)
                .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
            return buildSupervisedTask("fetch gf intraday hr", task, dateRange);
        };
        return runGFTaskUnderWatch(taskSupplier, gfTaskWatcher);
    }

    private Task<List<Session>> runReadRawSessionList(Pair<Date, Date> dateRange, SupervisedExecutor gfTaskWatcher) {
        Supplier<SupervisedTask<SessionReadResponse>> taskSupplier = () -> {
            Task<SessionReadResponse> task = readSessions(dateRange.first, dateRange.second);
            return buildSupervisedTask("fetch gf sessions", task, dateRange);
        };
        Task<List<Session>> readSessionsTask = runGFTaskUnderWatch(taskSupplier, gfTaskWatcher)
            .onSuccessTask(localBackgroundExecutor, (response) -> Tasks.forResult(response.getSessions()));
        return readSessionsTask;
    }

    @SuppressLint("NewApi")
    private Task<List<GFSessionBundle>> runReadDetailedSessionBundles(List<Session> sessions,
        SupervisedExecutor gfTaskWatcher) {
        // NOTE: this method runs all tasks sequentially because dedicated tasks reading session
        // samples must be executed before to continue the rest.
        // TODO: rewrite this piece via task suppliers and #runGFTaskUnderWatch
        List<Task<GFSessionBundle>> sessionBundlesTasks = new ArrayList<>();
        for (Session session : sessions) {
            Task<GFSessionBundle> taskToContinue = sessionBundlesTasks.isEmpty() ? Tasks.forResult(null)
                : sessionBundlesTasks.get(sessionBundlesTasks.size() - 1);
            Task<GFSessionBundle> nextTask = taskToContinue.continueWithTask(localBackgroundExecutor,
                (t) -> bundleSessionWithData(session, gfTaskWatcher));
            sessionBundlesTasks.add(nextTask);
        }
        Task<List<GFSessionBundle>> completedSessionBundlesTasks =
            Tasks.whenAll(sessionBundlesTasks).continueWithTask(localBackgroundExecutor, (commonResultTask) -> {
                if (commonResultTask.isCanceled() || !commonResultTask.isSuccessful()) {
                    return Tasks.forException(GoogleTaskUtils.extractGFExceptionFromTasks(sessionBundlesTasks)
                        .orElse(commonResultTask.getException()));
                }
                List<GFSessionBundle> bundleList =
                    sessionBundlesTasks.stream().map(t -> t.getResult()).collect(Collectors.toList());
                return Tasks.forResult(bundleList);
            });
        return completedSessionBundlesTasks;
    }

    private <T> Task<T> runGFTaskUnderWatch(Supplier<SupervisedTask<T>> taskSupplier,
        SupervisedExecutor gfTaskWatcher) {
        // NOTE: here was implementation with TaskCompletionSource but it didn't work in the test
        // environment because a call #cancel in other thread doesn't notify the listener in
        // TaskCompletionSource.
        final ExecutorService gfTaskWatcherExecutor = gfTaskWatcher.getExecutor();
        final CancellationToken cancellationToken = gfTaskWatcher.getCancellationToken();
        final CancellationTokenSource cancellationTokenSource = gfTaskWatcher.getCancellationTokenSource();

        return Tasks.forResult(null).continueWithTask(gfTaskWatcherExecutor, dumbTask -> {
            if (cancellationToken.isCancellationRequested()) {
                cancellationTokenSource.cancel();
                return Tasks.forCanceled();
            }
            final SupervisedTask<T> supervisedTask = taskSupplier.get();
            for (int tryNumber = 0; tryNumber <= supervisedTask.getRetriesCount()
                && !cancellationToken.isCancellationRequested(); tryNumber++) {
                try {
                    Task<T> originalTask = supervisedTask.getTask();
                    T result = Tasks.await(originalTask, supervisedTask.getTimeoutSeconds(), TimeUnit.SECONDS);
                    return Tasks.forResult(result);
                } catch (InterruptedException | TimeoutException exc) {
                    // expected exception due to either the task cancellation or timeout, give a try again
                    continue;
                } catch (ExecutionException exc) {
                    // exception originated from the completed task
                    Exception exception = new GoogleFitActivitySourceExceptions.CommonException(exc.getCause());
                    if (!cancellationToken.isCancellationRequested()) {
                        cancellationTokenSource.cancel();
                    }
                    return Tasks.forException(exception);
                }
            }
            if (cancellationToken.isCancellationRequested()) {
                return Tasks.forCanceled();
            }
            String exceptionMessage = String.format(Locale.US,
                "Possible tries count (%d) exceeded for task \"%s\"",
                supervisedTask.getRetriesCount() + 1,
                supervisedTask.getName());
            cancellationTokenSource.cancel();
            return Tasks.forException(new MaxTriesCountExceededException(exceptionMessage));
        });
    }

    @SuppressLint("NewApi")
    private <V, T extends List<V>> Task<T> flatMapTasksResults(List<Task<T>> tasks) {
        Task<T> collectResultsTask = Tasks.whenAll(tasks).continueWithTask(localBackgroundExecutor, commonResult -> {
            if (commonResult.isCanceled() || !commonResult.isSuccessful()) {
                CommonException fallbackException = new CommonException("Pooling task was canceled");
                Exception exception = GoogleTaskUtils.extractGFExceptionFromTasks(tasks).orElse(fallbackException);;
                return Tasks.forException(exception);
            }
            T flattenResults = (T) tasks.stream().flatMap(t -> t.getResult().stream()).collect(Collectors.toList());
            return Tasks.forResult(flattenResults);
        });
        return collectResultsTask;
    }

    @SuppressLint("NewApi")
    private Task<GFSessionBundle> bundleSessionWithData(Session session, SupervisedExecutor gfTaskWatcher) {
        TaskCompletionSource<GFSessionBundle> taskCompletionSource =
            new TaskCompletionSource<>(gfTaskWatcher.getCancellationToken());
        Supplier<SupervisedTask<SessionReadResponse>> readSessionTaskSupplier = () -> {
            SessionReadRequest detailedSessionReadRequest = buildDetailedSessionReadRequest(session);
            Task<SessionReadResponse> task = sessionsClient.readSession(detailedSessionReadRequest);
            String taskName = String.format(Locale.US, "fetch detailed gf session %s", session.getIdentifier());
            return new SupervisedTask<>(taskName,
                task,
                config.detailedSessionQueryTimeoutSeconds,
                config.detailedSessionQueryRetriesCount);
        };
        // NOTE: here we create the new SupervisedExecutor with its own cancellation token source
        // because it's expectable that the read detailed session request may silently fall with
        // a logging message about TransactionTooLarge. Otherwise, all rest pending tasks will
        // be canceled.
        SupervisedExecutor dedicatedReadSessionTaskWatcher =
            new SupervisedExecutor(gfTaskWatcher.getExecutor(), new CancellationTokenSource());
        Task<SessionReadResponse> readSessionUnderWatch =
            runGFTaskUnderWatch(readSessionTaskSupplier, dedicatedReadSessionTaskWatcher);
        Task<GFSessionBundle> collectSessionBundleTask =
            readSessionUnderWatch.onSuccessTask(localBackgroundExecutor, sessionReadResponse -> {
                GFSessionBundle gfSessionBundle = collectSessionBundleFromSessionResponse(sessionReadResponse);
                return Tasks.forResult(gfSessionBundle);
            });
        // NOTE: It is possible that GF Session and its samples are too big for passing it through IPC Binder
        // (TransactionTooLarge),
        // for example: android.os.TransactionTooLargeException: Error while delivering result of client request
        // SessionReadRequest.
        // For this rare case, we have to try to fetch all related data points one by one.
        Task<GFSessionBundle> rescueSessionBundleTask =
            collectSessionBundleTask.continueWithTask(localBackgroundExecutor, (task) -> {
                if (task.getException() instanceof MaxTriesCountExceededException) {
                    return collectSessionBundleWithDataReadRequests(session, gfTaskWatcher);
                }
                return task;
            });

        rescueSessionBundleTask.addOnCompleteListener(localBackgroundExecutor, (commonResultTask) -> {
            if (!commonResultTask.isSuccessful() || commonResultTask.isCanceled()) {
                taskCompletionSource.trySetException(commonResultTask.getException());
                return;
            }
            taskCompletionSource.trySetResult(commonResultTask.getResult());
        });
        return taskCompletionSource.getTask();
    }

    @SuppressLint("NewApi")
    private GFSessionBundle collectSessionBundleFromSessionResponse(SessionReadResponse response)
        throws CommonException {
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
                List<GFStepsDataPoint> steps =
                    convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToSteps);
                sessionBundleBuilder.setSteps(steps);
            } else if (dataType.equals(DataType.TYPE_SPEED)) {
                List<GFSpeedDataPoint> speed =
                    convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToSpeed);
                sessionBundleBuilder.setSpeed(speed);
            } else if (dataType.equals(DataType.TYPE_POWER_SAMPLE)) {
                List<GFPowerDataPoint> power =
                    convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToPower);
                sessionBundleBuilder.setPower(power);
            } else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                List<GFCalorieDataPoint> calories =
                    convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToCalorie);
                sessionBundleBuilder.setCalories(calories);
            } else if (dataType.equals(DataType.TYPE_ACTIVITY_SEGMENT)) {
                List<GFActivitySegmentDataPoint> activitySegments =
                    convertDataSetToPoints(dataSet, GFDataConverter::convertDataPointToActivitySegment);
                sessionBundleBuilder.setActivitySegments(activitySegments);
            }
        }
        return sessionBundleBuilder.build();
    }

    @SuppressLint("NewApi")
    private Task<GFSessionBundle> collectSessionBundleWithDataReadRequests(Session session,
        SupervisedExecutor gfTaskWatcher) {
        Date sessionStart = new Date(session.getStartTime(TimeUnit.MILLISECONDS));
        Date sessionEnd = new Date(session.getEndTime(TimeUnit.MILLISECONDS));
        List<Pair<Date, Date>> dateChunks =
            gfUtils.splitDateRangeIntoChunks(sessionStart, sessionEnd, Duration.ofMinutes(15));
        List<DataReadRequest> readHrRequests =
            buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_HEART_RATE_BPM);
        List<DataReadRequest> readStepsRequests =
            buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_STEP_COUNT_DELTA);
        List<DataReadRequest> readSpeedRequests = buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_SPEED);
        List<DataReadRequest> readPowerRequests =
            buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_POWER_SAMPLE);
        List<DataReadRequest> readCaloriesRequests =
            buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_CALORIES_EXPENDED);
        List<DataReadRequest> readSegmentsRequests =
            buildChunkedDataTypeReadRequests(dateChunks, DataType.TYPE_ACTIVITY_SEGMENT);

        Task<List<GFHRDataPoint>> getSessionHRTask = runAndConcatSoleDataReadRequests(readHrRequests,
            GFDataConverter::convertDataPointToHR,
            (req) -> String.format(Locale.US, "fetch gf HR for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFStepsDataPoint>> getSessionStepsTask = runAndConcatSoleDataReadRequests(readStepsRequests,
            GFDataConverter::convertDataPointToSteps,
            (req) -> String.format(Locale.US, "fetch gf steps for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFSpeedDataPoint>> getSessionSpeedTask = runAndConcatSoleDataReadRequests(readSpeedRequests,
            GFDataConverter::convertDataPointToSpeed,
            (req) -> String.format(Locale.US, "fetch gf speed for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFPowerDataPoint>> getSessionPowerTask = runAndConcatSoleDataReadRequests(readPowerRequests,
            GFDataConverter::convertDataPointToPower,
            (req) -> String.format(Locale.US, "fetch gf power for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFCalorieDataPoint>> getSessionCaloriesTask = runAndConcatSoleDataReadRequests(readCaloriesRequests,
            GFDataConverter::convertDataPointToCalorie,
            (req) -> String.format(Locale.US, "fetch gf calories for session %s", session.getIdentifier()),
            gfTaskWatcher);
        Task<List<GFActivitySegmentDataPoint>> getSessionSegmentsTask =
            runAndConcatSoleDataReadRequests(readSegmentsRequests,
                GFDataConverter::convertDataPointToActivitySegment,
                (req) -> String.format(Locale.US, "fetch gf activity segments for session %s", session.getIdentifier()),
                gfTaskWatcher);

        List<Task<?>> tasks = Arrays.asList(getSessionHRTask,
            getSessionStepsTask,
            getSessionSpeedTask,
            getSessionPowerTask,
            getSessionCaloriesTask,
            getSessionSegmentsTask);
        Task<GFSessionBundle> collectSessionBundleTask =
            Tasks.whenAll(tasks).continueWithTask(localBackgroundExecutor, (commonResultTask) -> {
                if (commonResultTask.isCanceled() || !commonResultTask.isSuccessful()) {
                    return Tasks.forException(
                        GoogleTaskUtils.extractGFExceptionFromTasks(tasks).orElse(commonResultTask.getException()));
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
    private <T extends GFDataPoint> Task<List<T>> runAndConcatSoleDataReadRequests(List<DataReadRequest> readRequests,
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
                    .onSuccessTask(localBackgroundExecutor, convertData.andThen(Tasks::forResult)::apply);
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
        sessionBundleBuilder.setType(zzfv.zza(session.getActivity()));
        sessionBundleBuilder.setActivityType(session.getActivity());
        return sessionBundleBuilder;
    }

    @SuppressLint("NewApi")
    private static <T extends GFDataPoint> List<T> convertDataSetToPoints(DataSet dataSet,
        Function<DataPoint, T> mapper) {
        return dataSet.getDataPoints().stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @SuppressLint("NewApi")
    @Nullable
    private static <T extends GFDataPoint> T convertResponseDataSetToPoint(@NonNull DataReadResponse response,
        @NonNull DataType type,
        Function<DataPoint, T> mapper) {
        final DataSet dataSet = response.getDataSet(type);
        if (dataSet.isEmpty()) {
            return null;
        }
        return mapper.apply(dataSet.getDataPoints().get(0));
    }

    @SuppressLint("NewApi")
    private static <T extends GFDataPoint> List<T> convertResponseBucketsToPoints(DataReadResponse response,
        Function<Bucket, T> mapper) {
        return response.getBuckets().stream().map(mapper).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private <T> SupervisedTask<T> buildSupervisedTask(String jobName, Task<T> task, Pair<Date, Date> dateRange) {
        Date start = dateRange.first;
        Date end = dateRange.second;
        String taskName = String.format(Locale.US,
            "'%s' for %s-%s",
            jobName,
            dateFormatter.get().format(start),
            dateFormatter.get().format(end));
        return new SupervisedTask<>(taskName, task, config.queryTimeoutSeconds, config.queryRetriesCount);
    }

    private Task<DataReadResponse> readAggregatedCaloriesHistory(Date start, Date end) {
        DataReadRequest request = new DataReadRequest.Builder().aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<DataReadResponse> readAggregatedStepsHistory(Date start, Date end) {
        final DataSource estimatedStepsDataSource = new DataSource.Builder().setAppPackageName("com.google.android.gms")
            .setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .build();
        DataReadRequest request = new DataReadRequest.Builder().aggregate(estimatedStepsDataSource)
            .bucketByTime(15, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<DataReadResponse> readAggregatedHRHistory(Date start, Date end) {
        DataReadRequest request = new DataReadRequest.Builder().aggregate(DataType.TYPE_HEART_RATE_BPM)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
        return historyClient.readData(request);
    }

    private Task<DataReadResponse> readLastKnownDataPointOfType(DataType type) {
        final DataReadRequest request = new DataReadRequest.Builder().read(type)
            .setTimeRange(1, new Date().getTime(), TimeUnit.MILLISECONDS)
            .setLimit(1)
            .build();
        return historyClient.readData(request);
    }

    private Task<SessionReadResponse> readSessions(Date start, Date end) {
        SessionReadRequest readRequest = buildSessionListReadRequest(start, end);
        return sessionsClient.readSession(readRequest);
    }

    private DataReadRequest buildDataTypeReadRequest(DataType type, Date start, Date end) {
        return new DataReadRequest.Builder().read(type)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }

    private SessionReadRequest buildSessionListReadRequest(Date start, Date end) {
        return new SessionReadRequest.Builder().setTimeInterval(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .readSessionsFromAllApps()
            .build();
    }

    private SessionReadRequest buildDetailedSessionReadRequest(Session session) {
        return new SessionReadRequest.Builder().readSessionsFromAllApps()
            .setTimeInterval(session.getStartTime(TimeUnit.MILLISECONDS),
                session.getEndTime(TimeUnit.MILLISECONDS),
                TimeUnit.MILLISECONDS)
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
        return Executors.newSingleThreadExecutor();
    }
}
