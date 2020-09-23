package com.fjuul.sdk.activitysources.entities;

import android.annotation.SuppressLint;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GoogleFitDataManager {
    private static final String TAG = "GoogleFitDataManager";

    private GoogleSignInAccount account;
    private HistoryClient historyClient;
    private GFDataUtils gfUtils;

    public GoogleFitDataManager(HistoryClient client, GoogleSignInAccount account, GFDataUtils gfUtils) {
        this.account = account;
        this.historyClient = client;
        this.gfUtils = gfUtils;
    }

    @SuppressLint("NewApi")
    public void syncCalories(Date start, Date end) {
        getCalories(start, end).continueWith((getCaloriesTask) -> {
            if (!getCaloriesTask.isSuccessful()) {
                throw  new Error("Couldn't get calories from GoogleFit Api");
            }
            List<GFCalorieDataPoint> calories = getCaloriesTask.getResult();
            this.gfUtils.groupPointsIntoBatchesByDuration(start, end, calories, Duration.ofMinutes(30));

            // TODO: dirty checks for calories data
            return null;
        });
    }

    public Task<List<GFCalorieDataPoint>> getCalories(Date start, Date end) {
        return readCaloriesHistory(start, end).continueWithTask(this::convertToCalories);
    }

    private Task<List<GFCalorieDataPoint>> convertToCalories(Task<DataReadResponse> task) {
        ArrayList<GFCalorieDataPoint> calorieDataPoints = new ArrayList<>();
        DataReadResponse dataReadResult = task.getResult();
        for (Bucket bucket : dataReadResult.getBuckets()) {
            Date start = new Date(bucket.getStartTime(TimeUnit.MILLISECONDS));
//            Log.d(TAG, "Bucket start time: " + new Date(bucket.getStartTime(TimeUnit.MILLISECONDS)));
//            Log.d(TAG, "Bucket end time: " + new Date(bucket.getEndTime(TimeUnit.MILLISECONDS)));
            DataSet dataSet = bucket.getDataSets().get(0);
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

    private Task<DataReadResponse> readCaloriesHistory(Date start, Date end) {
        DataReadRequest readRequest = buildCaloriesDataReadRequest(start, end);
        return historyClient.readData(readRequest);
    }

    private DataReadRequest buildCaloriesDataReadRequest(Date start, Date end) {
        return new DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .bucketByTime(1, TimeUnit.MINUTES)
            .setTimeRange(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .build();
    }
}
