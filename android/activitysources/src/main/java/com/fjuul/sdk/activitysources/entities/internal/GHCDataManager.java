package com.fjuul.sdk.activitysources.entities.internal;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.WeightRecord;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectProfileSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleHealthConnectSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCDataConverter;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCHeartRateSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlehealthconnect.GHCStepsDataPoint;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.core.utils.Logger;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GHCDataManager {
    private static final int MAX_DAYS = 30;  // Don't try to get older data than this
    private static final ExecutorService localBackgroundExecutor = Executors.newCachedThreadPool();

    private static final String LOG_TAG = "GHCDataManager";
    private static final String PROFILE_CHANGES_TOKEN_KEY = "ghc-profile-changes-token";
    private static final String INTRADAY_CHANGES_TOKEN_KEY = "ghc-intraday-changes-token";
    private static final String SESSION_CHANGES_TOKEN_KEY = "ghc-session-changes-token";

    private final @NonNull ActivitySourcesService activitySourcesService;
    private final @NonNull GHCClientWrapper clientWrapper;
    private final @NonNull ApiClient apiClient;

    public GHCDataManager(
        @NonNull GHCClientWrapper clientWrapper,
        @NonNull ActivitySourcesService activitySourcesService,
        @NonNull ApiClient apiClient) {
        this.clientWrapper = clientWrapper;
        this.activitySourcesService = activitySourcesService;
        this.apiClient = apiClient;
    }

    @NonNull
    public Task<Void> syncIntradayMetrics(GoogleHealthConnectIntradaySyncOptions options) {
        Log.d(LOG_TAG, "Syncing intraday metrics");
        final Task<Boolean> checkPermissionsTask = checkPermissions();
        final Task<HealthConnectRecords> readTask =
            checkPermissionsTask.continueWithTask(localBackgroundExecutor,
                task -> readIntradayMetrics(options.getMetrics()));
        return readTask.onSuccessTask(localBackgroundExecutor, (healthConnectRecords -> {
            final String nextToken = healthConnectRecords.getNextToken();
            final List<Record> healthRecords = healthConnectRecords.getRecords();
            if (healthRecords.isEmpty()) {
                Log.d(LOG_TAG, "No new records to send");
                return Tasks.forResult(null);
            } else {
                final List<GHCStepsDataPoint> stepsDataPoints = new ArrayList<>();
                final List<GHCCalorieDataPoint> calorieDataPoints = new ArrayList<>();
                final List<GHCHeartRateSummaryDataPoint> heartRateSummaryDataPoints =
                    new ArrayList<>();
                for (Record healthRecord : healthRecords) {
                    if (healthRecord instanceof StepsRecord stepsRecord) {
                        stepsDataPoints.add(GHCDataConverter.convertRecordToSteps(stepsRecord));
                    } else if (healthRecord instanceof HeartRateRecord heartRateRecord) {
                        heartRateSummaryDataPoints.add(
                            GHCDataConverter.convertRecordToHeartRateSummary(heartRateRecord));
                    } else if (healthRecord instanceof TotalCaloriesBurnedRecord totalCaloriesBurnedRecord) {
                        calorieDataPoints.add(
                            GHCDataConverter.convertRecordToCalories(totalCaloriesBurnedRecord));
                    } else {
                        Log.e(LOG_TAG, "Unexpected record type: " + healthRecord.getClass().getCanonicalName());
                    }
                }
                GHCUploadData uploadData = new GHCUploadData();
                uploadData.setStepsData(stepsDataPoints);
                uploadData.setHeartRateData(heartRateSummaryDataPoints);
                uploadData.setCaloriesData(calorieDataPoints);

                return sendGHCUploadData(INTRADAY_CHANGES_TOKEN_KEY, nextToken, uploadData).onSuccessTask(
                    localBackgroundExecutor, (apiCallResult) -> Tasks.forResult(apiCallResult.getValue()));
            }
        }));
    }

    @NonNull
    public Task<Void> syncSessions(GoogleHealthConnectSessionSyncOptions options) {
        Log.d(LOG_TAG, "Syncing sessions");
        final Task<Boolean> checkPermissionsTask = checkPermissions();
        final Task<HealthConnectSessions> readTask =
            checkPermissionsTask.continueWithTask(localBackgroundExecutor,
                task -> readExerciseSessions(options.getMinimumSessionDuration()));
        return readTask.onSuccessTask(localBackgroundExecutor, (healthConnectSessions -> {
            final String nextToken = healthConnectSessions.getNextToken();
            final List<ExerciseSession> exerciseSessions = healthConnectSessions.getSessions();
            if (exerciseSessions.isEmpty()) {
                Log.d(LOG_TAG, "No new sessions to send");
                return Tasks.forResult(null);
            } else {
                final List<GHCSessionBundle> sessionBundles = new ArrayList<>();
                for (ExerciseSession exerciseSession : exerciseSessions) {
                    sessionBundles.add(
                        GHCDataConverter.convertSessionToSessionBundle(exerciseSession));
                }
                GHCUploadData uploadData = new GHCUploadData();
                uploadData.setSessionsData(sessionBundles);

                return sendGHCUploadData(SESSION_CHANGES_TOKEN_KEY, nextToken, uploadData).onSuccessTask(
                    localBackgroundExecutor, (apiCallResult) -> Tasks.forResult(apiCallResult.getValue()));
            }
        }));
    }

    @NonNull
    public Task<Void> syncProfile(GoogleHealthConnectProfileSyncOptions options) {
        Log.d(LOG_TAG, "Syncing profile");
        final Task<Boolean> checkPermissionsTask = checkPermissions();
        final Task<HealthConnectRecords> readTask =
            checkPermissionsTask.continueWithTask(localBackgroundExecutor,
                task -> readProfileMetrics(options.getMetrics()));
        return readTask.onSuccessTask(localBackgroundExecutor, (healthConnectRecords -> {
            final String nextToken = healthConnectRecords.getNextToken();
            final List<Record> healthRecords = healthConnectRecords.getRecords();
            GHCSynchronizableProfileParams profileParams = new GHCSynchronizableProfileParams();
            // We have to go through all the height and weight records and select the most
            // recent values, if any
            Instant newestHeightTime = Instant.ofEpochMilli(0);  // very old
            Instant newestWeightTime = Instant.ofEpochMilli(0);  // very old
            for (Record healthRecord : healthRecords) {
                if (healthRecord instanceof HeightRecord heightRecord) {
                    Instant heightTime = heightRecord.getTime();
                    if (heightTime.isAfter(newestHeightTime)) {
                        newestHeightTime = heightTime;
                        profileParams.setHeight((float) (heightRecord.getHeight().getMeters()));
                    }
                } else if (healthRecord instanceof WeightRecord weightRecord) {
                    Instant weightTime = weightRecord.getTime();
                    if (weightTime.isAfter(newestWeightTime)) {
                        newestWeightTime = weightTime;
                        profileParams.setWeight((float) (weightRecord.getWeight().getKilograms()));
                    }
                } else {
                    Log.e(LOG_TAG, "Unexpected record type: " + healthRecord.getClass().getCanonicalName());
                }
            }
            if (profileParams.isEmpty()) {
                Log.d(LOG_TAG, "No new profile data to send");
                return Tasks.forResult(null);
            } else {
                return sendGHCProfileParams(nextToken, profileParams).onSuccessTask(
                    localBackgroundExecutor, (apiCallResult) -> Tasks.forResult(apiCallResult.getValue()));
            }
        }));
    }

    public Task<Boolean> checkPermissions() {
        HealthConnectAvailability availability = clientWrapper.getAvailability().getValue();
        if (availability != HealthConnectAvailability.INSTALLED) {
            Log.e(LOG_TAG, "Failed: not installed");
            return Tasks.forResult(false);
        }
        try {
            Boolean hasAllPermissions = clientWrapper.hasAllPermissionsAsync(
                clientWrapper.getDefaultRequiredPermissions()).get();
            if (!hasAllPermissions) {
                Log.e(LOG_TAG, "Failed: does not have required permissions");
                return Tasks.forResult(false);
            }
        } catch (ExecutionException ex) {
            Log.e(LOG_TAG, "Execution failed: " + ex.getMessage());
            return Tasks.forException(ex);
        } catch (InterruptedException ex) {
            Log.e(LOG_TAG, "Execution interrupted: " + ex.getMessage());
            return Tasks.forException(ex);
        }
        return Tasks.forResult(true);
    }

    @NonNull
    private Task<HealthConnectRecords> readIntradayMetrics(Set<FitnessMetricsType> optionsMetricTypes) {
        TaskCompletionSource<HealthConnectRecords> taskCompletionSource =
            new TaskCompletionSource<>();
        try {
            String token = apiClient.getStorage().get(INTRADAY_CHANGES_TOKEN_KEY);
            HealthConnectRecords records;
            if (token == null) {
                records = clientWrapper.getInitialIntradayRecordsAsync(MAX_DAYS, optionsMetricTypes).get();
            } else {
                records = clientWrapper.getIntradayChangeRecordsAsync(token, optionsMetricTypes).get();
            }
            taskCompletionSource.trySetResult(records);
        } catch (ExecutionException ex) {
            Log.e(LOG_TAG, "Execution failed: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        } catch (InterruptedException ex) {
            Log.e(LOG_TAG, "Execution interrupted: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        }
        return taskCompletionSource.getTask();
    }

    @NonNull
    private Task<HealthConnectSessions> readExerciseSessions(Duration optionsMaxDuration) {
        TaskCompletionSource<HealthConnectSessions> taskCompletionSource =
            new TaskCompletionSource<>();
        try {
            String token = apiClient.getStorage().get(SESSION_CHANGES_TOKEN_KEY);
            HealthConnectSessions sessions;
            if (token == null) {
                sessions = clientWrapper.getInitialSessionsAsync(MAX_DAYS, optionsMaxDuration).get();
            } else {
                sessions = clientWrapper.getChangeSessionsAsync(token, optionsMaxDuration).get();
            }
            taskCompletionSource.trySetResult(sessions);
        } catch (ExecutionException ex) {
            Log.e(LOG_TAG, "Execution failed: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        } catch (InterruptedException ex) {
            Log.e(LOG_TAG, "Execution interrupted: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        }
        return taskCompletionSource.getTask();
    }

    @NonNull
    private Task<HealthConnectRecords> readProfileMetrics(Set<FitnessMetricsType> optionsMetricTypes) {
        TaskCompletionSource<HealthConnectRecords> taskCompletionSource =
            new TaskCompletionSource<>();
        try {
            String token = apiClient.getStorage().get(PROFILE_CHANGES_TOKEN_KEY);
            HealthConnectRecords records;
            if (token == null) {
                records = clientWrapper.getInitialProfileRecordsAsync(MAX_DAYS, optionsMetricTypes).get();
            } else {
                records = clientWrapper.getProfileChangeRecordsAsync(token, optionsMetricTypes).get();
            }
            taskCompletionSource.trySetResult(records);
        } catch (ExecutionException ex) {
            Log.e(LOG_TAG, "Execution failed: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        } catch (InterruptedException ex) {
            Log.e(LOG_TAG, "Execution interrupted: " + ex.getMessage());
            taskCompletionSource.trySetException(ex);
        }
        return taskCompletionSource.getTask();
    }

    @NonNull
    private Task<ApiCallResult<Void>> sendGHCUploadData(@NonNull String tokenKey, @NonNull String nextToken, @NonNull GHCUploadData uploadData) {
        final TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        activitySourcesService.uploadGoogleHealthConnectData(uploadData).enqueue((apiCall, result) -> {
            if (result.isError()) {
                Log.d(LOG_TAG, "failed to send GHC data: " + result.getError().getMessage());
                final GoogleFitActivitySourceExceptions.CommonException exception =
                    new GoogleFitActivitySourceExceptions.CommonException("Failed to send data to the server", result.getError());
                sendDataTaskCompletionSource.trySetException(exception);
                return;
            }
            Log.d(LOG_TAG, "succeeded to send GHC data");
            apiClient.getStorage().set(tokenKey, nextToken);
            sendDataTaskCompletionSource.trySetResult(result);
        });
        return sendDataTaskCompletionSource.getTask();
    }

    @NonNull
    private Task<ApiCallResult<Void>> sendGHCProfileParams(@NonNull String nextToken, @NonNull GHCSynchronizableProfileParams profileParams) {
        final TaskCompletionSource<ApiCallResult<Void>> sendDataTaskCompletionSource = new TaskCompletionSource<>();
        activitySourcesService.updateProfileOnBehalfOfGoogleHealthConnect(profileParams).enqueue((apiCall, result) -> {
            if (result.isError()) {
                Logger.get().d("failed to send the profile data: %s", result.getError().getMessage());
                final GoogleFitActivitySourceExceptions.CommonException exception =
                    new GoogleFitActivitySourceExceptions.CommonException("Failed to send data to the server", result.getError());
                sendDataTaskCompletionSource.trySetException(exception);
                return;
            }
            Logger.get().d("succeeded to send the profile data");
            apiClient.getStorage().set(PROFILE_CHANGES_TOKEN_KEY, nextToken);
            sendDataTaskCompletionSource.trySetResult(result);
        });
        return sendDataTaskCompletionSource.getTask();
    }
}
