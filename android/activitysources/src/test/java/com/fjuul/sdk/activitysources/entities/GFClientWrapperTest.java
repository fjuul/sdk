package com.fjuul.sdk.activitysources.entities;

import android.os.Build;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException;
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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GFClientWrapperTest {

    static final GFClientWrapper.Config TEST_CONFIG = new GFClientWrapper.Config(0, 1, 0, 1);
    public static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();
    public static final ExecutorService testUtilExecutor = Executors.newCachedThreadPool();

    @AfterClass
    public static void shutdownExecutor() {
        testUtilExecutor.shutdownNow();
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static boolean isXMinutesBucketRequest(DataReadRequest request, int minutes) {
        return request.getBucketDuration(TimeUnit.MINUTES) == minutes;
    }

    public static boolean isTimeIntervalOfRequest(DataReadRequest request, Date start, Date end) {
        return new Date(request.getStartTime(TimeUnit.MILLISECONDS)).equals(start) &&
            new Date(request.getEndTime(TimeUnit.MILLISECONDS)).equals(end);
    }

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
            subject = new GFClientWrapper(TEST_CONFIG, mockedHistoryClient, mockedSessionsClient, gfDataUtilsSpy);
        }

        @Test
        public void getCalories_oneDay_returnsTaskWithCalories() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));
            DataPoint calorieDataPoint = createRawDataPoint(caloriesDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:41Z")),
                Field.FIELD_CALORIES,
                10f);
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
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg, start, end) &&
                    isXMinutesBucketRequest(arg, 1) &&
                    correctDataType;
            }));

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getCalories_fewDays_returnsTaskWithCalories() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            DataPoint calorieDataPoint1 = createRawDataPoint(caloriesDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:41Z")),
                Field.FIELD_CALORIES,
                10f);
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
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg, 1) &&
                    correctDataType;
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);

                return isTimeIntervalOfRequest(arg, Date.from(Instant.parse("2020-10-02T00:00:00Z")), end) &
                    isXMinutesBucketRequest(arg, 1) &&
                    correctDataType;
            }));

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getCalories_requestFewDaysButFirstDayCantBeDelivered_returnsTaskWithCalories() throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forException(new Exception("Application needs OAuth consent from the user")));
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) { }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(CommonException.class));
            CommonException gfException = (CommonException) exception;
            assertEquals("should have error message",
                "Application needs OAuth consent from the user",
                gfException.getCause().getMessage());
            // should request data only for the first day due to the serial execution
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat(arg -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);

                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg,1) &&
                    correctDataType;
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getCalories_requestFewDaysButFirstDayExceedTimeoutWithRetries_returnsTaskWithCalories() throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));

            when(mockedHistoryClient.readData(Mockito.any()))
                .thenAnswer(invocation -> {
                    // NOTE: here we're simulating the died request to GF
                    return Tasks.forResult(null).continueWithTask(testUtilExecutor, task -> {
                        Thread.sleep(5000);
                        return task;
                    });
                });
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            // catch expected ExecutionException
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) { }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException gfException = (MaxTriesCountExceededException) exception;
            assertThat("should have error message about the executed task",
                gfException.getMessage(),
                startsWith("Possible tries count (1) exceeded for task \"'fetch gf intraday calories' for 2020-10-01"));
            // should request data only for the first day due to the serial execution
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat(arg -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1 &&
                    arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg,start,Date.from(Instant.parse("2020-10-02T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg,1) &&
                    correctDataType;
            }));
            inOrder.verifyNoMoreInteractions();
            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }
    }

    public static class GetStepsTest extends GivenRobolectricContext {
        static final DataSource stepsDataSource = new DataSource.Builder()
            .setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED).build();
        GFClientWrapper subject;
        HistoryClient mockedHistoryClient;
        SessionsClient mockedSessionsClient;
        GFDataUtils gfDataUtilsSpy;

        public static boolean isRequestedCorrectDataSource(DataReadRequest request) {
            if (request.getAggregatedDataSources().isEmpty()) {
                return false;
            }
            DataSource dataSource = request.getAggregatedDataSources().get(0);
            return dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA) &&
                dataSource.getStreamName().equals("estimated_steps");
        }

        @Before
        public void beforeTests() {
            mockedHistoryClient = mock(HistoryClient.class);
            mockedSessionsClient = mock(SessionsClient.class);
            GFDataUtils gfDataUtils = new GFDataUtils();
            gfDataUtilsSpy = spy(gfDataUtils);
            subject = new GFClientWrapper(TEST_CONFIG, mockedHistoryClient, mockedSessionsClient, gfDataUtilsSpy);
        }

        @Test
        public void getSteps_oneDay_returnsTaskWithSteps() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));
            DataPoint rawSteps = createRawDataPoint(stepsDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:09:21Z")),
                Field.FIELD_STEPS,
                218);
            Date bucketStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            Date bucketEnd = Date.from(Instant.parse("2020-10-01T10:15:00Z"));
            Bucket mockedBucket = createMockedSoleBucket(
                bucketStart,
                bucketEnd,
                stepsDataSource,
                rawSteps
            );
            DataReadResponse dataReadResponse = createTestDataReadResponse(mockedBucket);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(dataReadResponse));
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFStepsDataPoint> steps = result.getResult();
            GFStepsDataPoint stepsDataPoint = steps.get(0);
            assertEquals("should convert raw data point to steps",
                "derived:com.google.step_count.delta:",
                stepsDataPoint.getDataSource());
            assertEquals("should take the start of bucket as start time of the steps",
                bucketStart,
                stepsDataPoint.getStart());
            assertNull("steps should have null end time",
                stepsDataPoint.getEnd());
            assertEquals("steps should have the number value",
                (Integer)218,
                stepsDataPoint.getValue());

            verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, end) &&
                    isXMinutesBucketRequest(arg, 15) &&
                    isRequestedCorrectDataSource(arg);
            }));

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }

        @Test
        public void getSteps_fewWeeks_returnsTaskWithSteps() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-13T23:59:59.999Z"));
            DataPoint rawStepsWeek1 = createRawDataPoint(stepsDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:09:21Z")),
                Field.FIELD_STEPS,
                218);
            DataPoint rawStepsWeek2 = createRawDataPoint(stepsDataSource,
                Date.from(Instant.parse("2020-10-08T12:00:05Z")),
                Date.from(Instant.parse("2020-10-08T12:13:44Z")),
                Field.FIELD_STEPS,
                740);
            Bucket mockedBucket1 = createMockedSoleBucket(
                Date.from(Instant.parse("2020-10-01T10:00:00Z")),
                Date.from(Instant.parse("2020-10-01T10:15:00Z")),
                stepsDataSource,
                rawStepsWeek1
            );
            Bucket mockedBucket2 = createMockedSoleBucket(
                Date.from(Instant.parse("2020-10-08T12:00:00Z")),
                Date.from(Instant.parse("2020-10-08T12:15:00Z")),
                stepsDataSource,
                rawStepsWeek2
            );
            DataReadResponse week1Response = createTestDataReadResponse(mockedBucket1);
            DataReadResponse week2Response = createTestDataReadResponse(mockedBucket2);
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forResult(week1Response))
                .thenReturn(Tasks.forResult(week2Response));
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFStepsDataPoint> totalSteps = result.getResult();
            assertEquals("should concatenate results of 2 responses", 2, totalSteps.size());
            GFStepsDataPoint week1Steps = totalSteps.get(0);
            assertEquals("should return steps in the order of responses",
                (Integer)218,
                week1Steps.getValue());
            GFStepsDataPoint week2Steps = totalSteps.get(1);
            assertEquals("should return steps in the order of responses",
                (Integer)740,
                week2Steps.getValue());
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg, 15) &&
                    isRequestedCorrectDataSource(arg);
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, Date.from(Instant.parse("2020-10-08T00:00:00Z")), end) &&
                    isXMinutesBucketRequest(arg, 15) &&
                    isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }

        @Test
        public void getSteps_requestFewWeeksButFirstWeekCantBeDelivered_returnsTaskWithSteps() throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-13T23:59:59.999Z"));
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forException(new Exception("Application needs OAuth consent from the user")));
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) { }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(CommonException.class));
            CommonException gfException = (CommonException) exception;
            assertEquals("should have error message",
                "Application needs OAuth consent from the user",
                gfException.getCause().getMessage());
            // should request data only for the first week due to the serial execution
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat(arg -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg,15) &&
                    isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }

        @Test
        public void getSteps_requestFewWeeksButFirstWeekExceedTimeoutWithRetries_returnsTaskWithSteps() throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-13T23:59:59.999Z"));

            when(mockedHistoryClient.readData(Mockito.any()))
                .thenAnswer(invocation -> {
                    // NOTE: here we're simulating the died request to GF
                    return Tasks.forResult(null).continueWithTask(testUtilExecutor, task -> {
                        Thread.sleep(5000);
                        return task;
                    });
                });
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            // catch expected ExecutionException
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) { }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException gfException = (MaxTriesCountExceededException) exception;
            assertThat("should have error message about the executed task",
                gfException.getMessage(),
                startsWith("Possible tries count (1) exceeded for task \"'fetch gf intraday steps' for 2020-10-01"));
            // should request data only for the first week due to the serial execution
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat(arg -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z"))) &&
                    isXMinutesBucketRequest(arg,15) &&
                    isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();
            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
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

    public static DataPoint createRawDataPoint(DataSource source, Date start, Date end, Field field, int value) {
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
