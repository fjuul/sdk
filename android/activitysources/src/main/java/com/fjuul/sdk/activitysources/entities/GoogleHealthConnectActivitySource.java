package com.fjuul.sdk.activitysources.entities;

import android.content.Context;
//TODO: Replace Log with Timber and use SDK's Timber utility classes
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.HeightRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.WeightRecord;

import com.fjuul.sdk.activitysources.entities.internal.GHCClientWrapper;
import com.fjuul.sdk.activitysources.entities.internal.HealthConnectAvailability;
import com.fjuul.sdk.activitysources.entities.internal.HealthConnectRecords;
import com.fjuul.sdk.core.ApiClient;

import java.util.concurrent.ExecutionException;

//TODO: Make this extend ActivitySource and uncomment getTrackerValue
public class GoogleHealthConnectActivitySource {
    private static final String LOG_TAG = "GoogleHealthConnectActivitySource";
    private static final String CHANGES_TOKEN_KEY = "google-health-connect-changes-token";
    private static volatile GoogleHealthConnectActivitySource instance;
    @NonNull
    private final ApiClient apiClient;
    @NonNull
    private final Context context;
    @NonNull
    private final ActivitySourcesManagerConfig config;
    @NonNull
    private final GHCClientWrapper clientWrapper;

    static synchronized void initialize(@NonNull ApiClient client,
                                        @NonNull ActivitySourcesManagerConfig config) {
        instance = new GoogleHealthConnectActivitySource(client, config);
    }

    private GoogleHealthConnectActivitySource(@NonNull ApiClient apiClient,
                                              @NonNull ActivitySourcesManagerConfig config) {
        this.apiClient = apiClient;
        this.context = apiClient.getAppContext();
        this.config = config;
        this.clientWrapper = new GHCClientWrapper(context);
    }

    @NonNull
    public static GoogleHealthConnectActivitySource getInstance() {
        return instance;
    }

//    @NonNull
//    @Override
//    protected TrackerValue getTrackerValue() {
//        return TrackerValue.GOOGLE_HEALTH_CONNECT;
//    }

    // TODO: integrate this with the SDK's framework so that it sends the changes as JSON
    // to the new endpoints
    public void checkForChanges() {
        new Thread(() -> {
            try {
                HealthConnectAvailability availability = clientWrapper.getAvailability().getValue();
                if (availability != HealthConnectAvailability.INSTALLED) {
                    Log.e(LOG_TAG, "Failed: not installed");
                    return;
                }
                Boolean hasAllPermissions = clientWrapper.hasAllPermissionsAsync(
                    clientWrapper.getDefaultRequiredPermissions()).get();
                if (!hasAllPermissions) {
                    Log.e(LOG_TAG, "Failed: does not have required permissions");
                    return;
                }
                String token = apiClient.getStorage().get(CHANGES_TOKEN_KEY);
                HealthConnectRecords records;
                if (token == null) {
                    records = clientWrapper.getInitialRecordsAsync(30).get();
                } else {
                    records = clientWrapper.getChangesAsync(token).get();
                }
                apiClient.getStorage().set(CHANGES_TOKEN_KEY, records.getNextToken());
                for (Record record : records.getRecords()) {
                    if (record instanceof HeightRecord) {
                        processHeightChange((HeightRecord)record);
                    } else if (record instanceof WeightRecord) {
                        processWeightChange((WeightRecord)record);
                    } else if (record instanceof StepsRecord) {
                        processStepsChange((StepsRecord)record);
                    } else if (record instanceof HeartRateRecord) {
                        processHeartRateChange((HeartRateRecord)record);
                    } else if (record instanceof TotalCaloriesBurnedRecord) {
                        processTotalCaloriesBurnedChange((TotalCaloriesBurnedRecord)record);
                    } else if (record instanceof ExerciseSessionRecord) {
                        processExerciseSessionChange((ExerciseSessionRecord)record);
                    } else {
                        Log.e(LOG_TAG, "Unexpected record type: " + record.getClass().getCanonicalName());
                    }
                }
            } catch (ExecutionException ex) {
                Log.e(LOG_TAG, "Threw ExecutionException: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Log.e(LOG_TAG, "Threw InterruptedException: " + ex.getMessage());
            }
        }).start();
    }

    private void processHeightChange(HeightRecord record) {
        Log.i(LOG_TAG, "Got height record, height: " + record.getHeight().getMeters() + "m");
    }

    private void processWeightChange(WeightRecord record) {
        Log.i(LOG_TAG, "Got weight record, weight: " + record.getWeight().getKilograms() + "kg");
    }

    private void processStepsChange(StepsRecord record) {
        Log.i(LOG_TAG, "Got steps record, steps: " + record.getCount() + " steps");
    }

    private void processHeartRateChange(HeartRateRecord record) {
        Log.i(LOG_TAG, "Got heart rate record, heart rate: " + record.getSamples().size() + " samples");
    }

    private void processTotalCaloriesBurnedChange(TotalCaloriesBurnedRecord record) {
        Log.i(LOG_TAG, "Got total calories burned record, burned: " + record.getEnergy().getCalories() + " calories");
    }

    private void processExerciseSessionChange(ExerciseSessionRecord record) {
        Log.i(LOG_TAG, "Got exercise session record, title: " + record.getTitle());
    }
}
