package com.fjuul.sdk.activitysources.entities;

import android.os.Build;

import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GFClientWrapperTest {

    public static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static class GetCaloriesTest extends GivenRobolectricContext {
        static final DataSource caloriesDataSource = new DataSource.Builder()
            .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
            .setType(DataSource.TYPE_DERIVED).build();
        GFClientWrapper subject;
        HistoryClient mockedHistoryClient;
        SessionsClient mockedSessionsClient;
        GFDataUtils gfDataUtilsSpy;

        @Before
        public void beforeTests() {
            mockedHistoryClient = mock(HistoryClient.class);
            mockedSessionsClient = mock(SessionsClient.class);
            GFDataUtils gfDataUtils = new GFDataUtils();
            gfDataUtilsSpy = spy(gfDataUtils);
            subject = new GFClientWrapper(mockedHistoryClient, mockedSessionsClient, gfDataUtilsSpy);
        }

        @Test
        public void getCalories_oneDay_returnsTaskWithCalories() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));
            DataPoint calorieDataPoint = createRawDataPoint(caloriesDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:41Z")),
                Field.FIELD_CALORIES,
                10);
            Date bucketStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            Date bucketEnd = Date.from(Instant.parse("2020-10-01T10:01:00Z"));
            Bucket mockedBucket = createMockedSoleBucket(
                bucketStart,
                bucketEnd,
                caloriesDataSource,
                calorieDataPoint
            );
            DataReadResponse dataReadResponse = createTestDataReadResponse(mockedBucket);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(dataReadResponse));
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFCalorieDataPoint> calories = result.getResult();
            GFCalorieDataPoint calorie = calories.get(0);
            assertEquals("should convert raw data point to calorie",
                "derived:com.google.calories.expended:",
                calorie.getDataSource());
            assertEquals("should take the start of bucket as start time of the calorie",
                bucketStart,
                calorie.getStart());
            assertNull("calorie should have null end time",
                calorie.getEnd());
            assertEquals("calorie should have kcals",
                10f,
                calorie.getValue(),
                0.0001);

            verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDates = new Date(arg.getStartTime(TimeUnit.MILLISECONDS)).equals(start) &&
                    new Date(arg.getEndTime(TimeUnit.MILLISECONDS)).equals(end);
                boolean bucketInOneMinutes = arg.getBucketDuration(TimeUnit.SECONDS) == 60;
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return correctDates && bucketInOneMinutes && correctDataType;
            }));

            // TODO: verify spy on 24h duration
        }

        @Test
        public void getCalories_fewDays_returnsTaskWithCalories() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            DataPoint calorieDataPoint1 = createRawDataPoint(caloriesDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:41Z")),
                Field.FIELD_CALORIES,
                10);
            DataPoint calorieDataPoint2 = createRawDataPoint(caloriesDataSource,
                Date.from(Instant.parse("2020-10-02T10:00:05Z")),
                Date.from(Instant.parse("2020-10-02T10:00:41Z")),
                Field.FIELD_CALORIES,
                7.21f);
            Bucket mockedBucket1 = createMockedSoleBucket(
                Date.from(Instant.parse("2020-10-01T10:00:00Z")),
                Date.from(Instant.parse("2020-10-01T10:01:00Z")),
                caloriesDataSource,
                calorieDataPoint1
            );
            Bucket mockedBucket2 = createMockedSoleBucket(
                Date.from(Instant.parse("2020-10-02T10:00:00Z")),
                Date.from(Instant.parse("2020-10-02T10:01:00Z")),
                caloriesDataSource,
                calorieDataPoint2
            );
            DataReadResponse day1Response = createTestDataReadResponse(mockedBucket1);
            DataReadResponse day2Response = createTestDataReadResponse(mockedBucket2);
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forResult(day1Response))
                .thenReturn(Tasks.forResult(day2Response));
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFCalorieDataPoint> calories = result.getResult();
            assertEquals("should concatenate results of 2 responses", 2, calories.size());
            GFCalorieDataPoint day1Calorie = calories.get(0);
            assertEquals("should return calories in the order of responses",
                10f,
                day1Calorie.getValue(),
                0.0001);
            GFCalorieDataPoint day2Calorie = calories.get(1);
            assertEquals("should return calories in the order of responses",
                7.21f,
                day2Calorie.getValue(),
                0.0001);
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDates = new Date(arg.getStartTime(TimeUnit.MILLISECONDS)).equals(start) &&
                    new Date(arg.getEndTime(TimeUnit.MILLISECONDS)).equals(
                        Date.from(Instant.parse("2020-10-02T00:00:00Z"))
                    );
                boolean bucketInOneMinutes = arg.getBucketDuration(TimeUnit.SECONDS) == 60;
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return correctDates && bucketInOneMinutes && correctDataType;
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDates =
                    new Date(arg.getStartTime(TimeUnit.MILLISECONDS)).equals(Date.from(Instant.parse("2020-10-02T00:00:00Z"))) &&
                    new Date(arg.getEndTime(TimeUnit.MILLISECONDS)).equals(end);
                boolean bucketInOneMinutes = arg.getBucketDuration(TimeUnit.SECONDS) == 60;
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return correctDates && bucketInOneMinutes && correctDataType;
            }));

            // TODO: verify spy on 24h duration
        }
    }

    public static Bucket createMockedSoleBucket(Date start, Date end, DataSource source, DataPoint... dataPoints) {
        Bucket mockedBucket = mock(Bucket.class);
        DataSet ds = DataSet.builder(source).addAll(Arrays.asList(dataPoints)).build();
        when(mockedBucket.getDataSets()).thenReturn(Arrays.asList(ds));
        when(mockedBucket.getStartTime(TimeUnit.MILLISECONDS)).thenReturn(start.getTime());
        when(mockedBucket.getEndTime(TimeUnit.MILLISECONDS)).thenReturn(end.getTime());
        return mockedBucket;
    }

    public static DataPoint createRawDataPoint(DataSource source, Date start, Date end, Field field, float value) {
        return DataPoint.builder(source)
            .setTimeInterval(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .setField(field, value)
            .build();
    }

    public static DataReadResponse createTestDataReadResponse(Bucket... buckets) {
        DataReadResponse dataReadResponse = new DataReadResponse();
        DataReadResult mockedDataReadResult = mock(DataReadResult.class);
        when(mockedDataReadResult.getBuckets()).thenReturn(Arrays.asList(buckets));
        dataReadResponse.setResult(mockedDataReadResult);
        return dataReadResponse;
    }
}
