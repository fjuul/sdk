package com.fjuul.sdk.activitysources.entities.internal;

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

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFActivitySegmentDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFPowerDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSpeedDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException;
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
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.os.Build;

@RunWith(Enclosed.class)
public class GFClientWrapperTest {

    static final GFClientWrapper.Config TEST_CONFIG = new GFClientWrapper.Config(0, 1, 0, 1);
    public static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();
    public static final ExecutorService testUtilExecutor = Executors.newCachedThreadPool();

    @AfterClass
    public static void shutdownExecutor() {
        testUtilExecutor.shutdownNow();
        testExecutor.shutdownNow();
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static boolean isXMinutesBucketRequest(DataReadRequest request, int minutes) {
        return request.getBucketDuration(TimeUnit.MINUTES) == minutes;
    }

    public static boolean isTimeIntervalOfRequest(DataReadRequest request, Date start, Date end) {
        return new Date(request.getStartTime(TimeUnit.MILLISECONDS)).equals(start)
            && new Date(request.getEndTime(TimeUnit.MILLISECONDS)).equals(end);
    }

    public static boolean isTimeIntervalOfRequest(SessionReadRequest request, Date start, Date end) {
        return new Date(request.getStartTime(TimeUnit.MILLISECONDS)).equals(start)
            && new Date(request.getEndTime(TimeUnit.MILLISECONDS)).equals(end);
    }

    public static class GetCaloriesTest extends GivenRobolectricContext {
        static final DataSource caloriesDataSource =
            new DataSource.Builder().setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                .setType(DataSource.TYPE_DERIVED)
                .build();
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
            Bucket mockedBucket = createMockedSoleBucket(bucketStart, bucketEnd, caloriesDataSource, calorieDataPoint);
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
            assertNull("calorie should have null end time", calorie.getEnd());
            assertEquals("calorie should have kcals", 10f, calorie.getValue(), 0.0001);

            verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1
                    && arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg, start, end) && isXMinutesBucketRequest(arg, 1) && correctDataType;
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
            Bucket mockedBucket1 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-01T10:00:00Z")),
                Date.from(Instant.parse("2020-10-01T10:01:00Z")),
                caloriesDataSource,
                calorieDataPoint1);
            Bucket mockedBucket2 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-02T10:00:00Z")),
                Date.from(Instant.parse("2020-10-02T10:01:00Z")),
                caloriesDataSource,
                calorieDataPoint2);
            DataReadResponse day1Response = createTestDataReadResponse(mockedBucket1);
            DataReadResponse day2Response = createTestDataReadResponse(mockedBucket2);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(day1Response))
                .thenReturn(Tasks.forResult(day2Response));
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFCalorieDataPoint> calories = result.getResult();
            assertEquals("should concatenate results of 2 responses", 2, calories.size());
            GFCalorieDataPoint day1Calorie = calories.get(0);
            assertEquals("should return calories in the order of responses", 10f, day1Calorie.getValue(), 0.0001);
            GFCalorieDataPoint day2Calorie = calories.get(1);
            assertEquals("should return calories in the order of responses", 7.21f, day2Calorie.getValue(), 0.0001);
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1
                    && arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && correctDataType;
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1
                    && arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);

                return isTimeIntervalOfRequest(arg, Date.from(Instant.parse("2020-10-02T00:00:00Z")), end)
                    & isXMinutesBucketRequest(arg, 1) && correctDataType;
            }));

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getCalories_requestFewDaysButFirstDayCantBeDelivered_returnsFailedTask()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forException(new Exception("Application needs OAuth consent from the user")));
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

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
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1
                    && arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);

                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && correctDataType;
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getCalories_requestFewDaysButFirstDayExceedTimeoutWithRetries_returnsFailedTask()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));

            when(mockedHistoryClient.readData(Mockito.any())).thenAnswer(invocation -> {
                // NOTE: here we're simulating the dead request to GF
                return Tasks.forResult(null).continueWithTask(testUtilExecutor, task -> {
                    Thread.sleep(5000);
                    return task;
                });
            });
            Task<List<GFCalorieDataPoint>> result = subject.getCalories(start, end);

            // catch expected ExecutionException
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

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
                boolean correctDataType = arg.getAggregatedDataTypes().size() == 1
                    && arg.getAggregatedDataTypes().contains(DataType.TYPE_CALORIES_EXPENDED);
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && correctDataType;
            }));
            inOrder.verifyNoMoreInteractions();
            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }
    }

    public static class GetStepsTest extends GivenRobolectricContext {
        static final DataSource stepsDataSource =
            new DataSource.Builder().setDataType(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .build();
        GFClientWrapper subject;
        HistoryClient mockedHistoryClient;
        SessionsClient mockedSessionsClient;
        GFDataUtils gfDataUtilsSpy;

        public static boolean isRequestedCorrectDataSource(DataReadRequest request) {
            if (request.getAggregatedDataSources().isEmpty()) {
                return false;
            }
            DataSource dataSource = request.getAggregatedDataSources().get(0);
            return dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)
                && dataSource.getStreamName().equals("estimated_steps");
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
            Bucket mockedBucket = createMockedSoleBucket(bucketStart, bucketEnd, stepsDataSource, rawSteps);
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
            assertNull("steps should have null end time", stepsDataPoint.getEnd());
            assertEquals("steps should have the number value", (Integer) 218, stepsDataPoint.getValue());

            verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, end) && isXMinutesBucketRequest(arg, 15)
                    && isRequestedCorrectDataSource(arg);
            }));

            // should split input date ranges into 7-day chunks
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
            Bucket mockedBucket1 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-01T10:00:00Z")),
                Date.from(Instant.parse("2020-10-01T10:15:00Z")),
                stepsDataSource,
                rawStepsWeek1);
            Bucket mockedBucket2 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-08T12:00:00Z")),
                Date.from(Instant.parse("2020-10-08T12:15:00Z")),
                stepsDataSource,
                rawStepsWeek2);
            DataReadResponse week1Response = createTestDataReadResponse(mockedBucket1);
            DataReadResponse week2Response = createTestDataReadResponse(mockedBucket2);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(week1Response))
                .thenReturn(Tasks.forResult(week2Response));
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFStepsDataPoint> totalSteps = result.getResult();
            assertEquals("should concatenate results of 2 responses", 2, totalSteps.size());
            GFStepsDataPoint week1Steps = totalSteps.get(0);
            assertEquals("should return steps in the order of responses", (Integer) 218, week1Steps.getValue());
            GFStepsDataPoint week2Steps = totalSteps.get(1);
            assertEquals("should return steps in the order of responses", (Integer) 740, week2Steps.getValue());
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 15) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, Date.from(Instant.parse("2020-10-08T00:00:00Z")), end)
                    && isXMinutesBucketRequest(arg, 15) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }

        @Test
        public void getSteps_requestFewWeeksButFirstWeekCantBeDelivered_returnsFailedTask()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-13T23:59:59.999Z"));
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forException(new Exception("Application needs OAuth consent from the user")));
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

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
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 15) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }

        @Test
        public void getSteps_requestFewWeeksButFirstWeekExceedTimeoutWithRetries_returnsFailedTask()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-13T23:59:59.999Z"));

            when(mockedHistoryClient.readData(Mockito.any())).thenAnswer(invocation -> {
                // NOTE: here we're simulating the dead request to GF
                return Tasks.forResult(null).continueWithTask(testUtilExecutor, task -> {
                    Thread.sleep(5000);
                    return task;
                });
            });
            Task<List<GFStepsDataPoint>> result = subject.getSteps(start, end);

            // catch expected ExecutionException
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

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
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-08T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 15) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();
            // should split input date ranges into 7-day chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(7));
        }
    }

    public static class GetHRSummariesTest extends GivenRobolectricContext {
        static final DataSource hrDataSource =
            new DataSource.Builder().setDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .setType(DataSource.TYPE_DERIVED)
                .build();
        GFClientWrapper subject;
        HistoryClient mockedHistoryClient;
        SessionsClient mockedSessionsClient;
        GFDataUtils gfDataUtilsSpy;

        public static boolean isRequestedCorrectDataSource(DataReadRequest request) {
            return request.getAggregatedDataTypes().contains(DataType.TYPE_HEART_RATE_BPM);
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
        public void getHRSummaries_oneDay_returnsTaskWithHR() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));
            DataPoint rawSteps = createAggregatedHRDataPoint(hrDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:21Z")),
                72,
                73,
                72.5f);
            Date bucketStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            Date bucketEnd = Date.from(Instant.parse("2020-10-01T10:01:00Z"));
            Bucket mockedBucket = createMockedSoleBucket(bucketStart, bucketEnd, hrDataSource, rawSteps);
            DataReadResponse dataReadResponse = createTestDataReadResponse(mockedBucket);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(dataReadResponse));
            Task<List<GFHRSummaryDataPoint>> result = subject.getHRSummaries(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFHRSummaryDataPoint> hrPoints = result.getResult();
            GFHRSummaryDataPoint hrSummary = hrPoints.get(0);
            assertEquals("should convert raw data point to hr summary",
                "derived:com.google.heart_rate.summary:",
                hrSummary.getDataSource());
            assertEquals("should take the start of bucket as start time of the data point",
                bucketStart,
                hrSummary.getStart());
            assertNull("data point should have null end time", hrSummary.getEnd());
            assertEquals("data point should have min value", 72, hrSummary.getMin(), 0.00001);
            assertEquals("data point should have max value", 73, hrSummary.getMax(), 0.00001);
            assertEquals("data point should have avg value", 72.5f, hrSummary.getAvg(), 0.00001);

            verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, end) && isXMinutesBucketRequest(arg, 1)
                    && isRequestedCorrectDataSource(arg);
            }));

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getHRSummaries_fewDays_returnsTaskWithHR() throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            DataPoint rawHRSummary1 = createAggregatedHRDataPoint(hrDataSource,
                Date.from(Instant.parse("2020-10-01T10:00:05Z")),
                Date.from(Instant.parse("2020-10-01T10:00:21Z")),
                72,
                73,
                72.5f);
            DataPoint rawHRSummary2 = createAggregatedHRDataPoint(hrDataSource,
                Date.from(Instant.parse("2020-10-02T10:00:15Z")),
                Date.from(Instant.parse("2020-10-02T10:00:45Z")),
                60,
                62,
                61);
            Bucket mockedBucket1 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-01T10:00:00Z")),
                Date.from(Instant.parse("2020-10-01T10:01:00Z")),
                hrDataSource,
                rawHRSummary1);
            Bucket mockedBucket2 = createMockedSoleBucket(Date.from(Instant.parse("2020-10-02T10:00:00Z")),
                Date.from(Instant.parse("2020-10-02T10:01:00Z")),
                hrDataSource,
                rawHRSummary2);
            DataReadResponse day1Response = createTestDataReadResponse(mockedBucket1);
            DataReadResponse day2Response = createTestDataReadResponse(mockedBucket2);
            when(mockedHistoryClient.readData(Mockito.any())).thenReturn(Tasks.forResult(day1Response))
                .thenReturn(Tasks.forResult(day2Response));
            Task<List<GFHRSummaryDataPoint>> result = subject.getHRSummaries(start, end);

            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFHRSummaryDataPoint> hrSummaries = result.getResult();
            assertEquals("should concatenate results of 2 responses", 2, hrSummaries.size());
            GFHRSummaryDataPoint day1HRSummary = hrSummaries.get(0);
            assertEquals("should return HR in the order of responses", 72.5, day1HRSummary.getAvg(), 0.00001);
            GFHRSummaryDataPoint day2HRSummary = hrSummaries.get(1);
            assertEquals("should return HR in the order of responses", 61, day2HRSummary.getAvg(), 0.00001);
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verify(mockedHistoryClient).readData(argThat((arg) -> {
                return isTimeIntervalOfRequest(arg, Date.from(Instant.parse("2020-10-02T00:00:00Z")), end)
                    && isXMinutesBucketRequest(arg, 1) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getHRSummaries_requestFewDaysButFirstDayCantBeDelivered_returnsFailedTask()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));
            when(mockedHistoryClient.readData(Mockito.any()))
                .thenReturn(Tasks.forException(new Exception("Application needs OAuth consent from the user")));
            Task<List<GFHRSummaryDataPoint>> result = subject.getHRSummaries(start, end);

            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

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
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }

        @Test
        public void getHRSummaries_requestFewDaysButFirstDayExceedTimeoutWithRetries_returnsFailedTasks()
            throws InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-02T23:59:59.999Z"));

            when(mockedHistoryClient.readData(Mockito.any())).thenAnswer(invocation -> {
                // NOTE: here we're simulating the dead request to GF
                return Tasks.forResult(null).continueWithTask(testUtilExecutor, task -> {
                    Thread.sleep(5000);
                    return task;
                });
            });
            Task<List<GFHRSummaryDataPoint>> result = subject.getHRSummaries(start, end);

            // catch expected ExecutionException
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {}

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException gfException = (MaxTriesCountExceededException) exception;
            assertThat("should have error message about the executed task",
                gfException.getMessage(),
                startsWith("Possible tries count (1) exceeded for task \"'fetch gf intraday hr' for 2020-10-01"));
            // should request data only for the first week due to the serial execution
            InOrder inOrder = inOrder(mockedHistoryClient);
            inOrder.verify(mockedHistoryClient).readData(argThat(arg -> {
                return isTimeIntervalOfRequest(arg, start, Date.from(Instant.parse("2020-10-02T00:00:00Z")))
                    && isXMinutesBucketRequest(arg, 1) && isRequestedCorrectDataSource(arg);
            }));
            inOrder.verifyNoMoreInteractions();
            // should split input date ranges into 24-hour chunks
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofHours(24));
        }
    }

    public static class GetSessionsTest extends GivenRobolectricContext {
        static final DataSource hrDataSource =
            new DataSource.Builder().setDataType(DataType.TYPE_HEART_RATE_BPM).setType(DataSource.TYPE_DERIVED).build();
        static final DataSource stepsDataSource = new DataSource.Builder().setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .build();
        static final DataSource caloriesDataSource =
            new DataSource.Builder().setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setType(DataSource.TYPE_DERIVED)
                .build();
        static final DataSource speedDataSource =
            new DataSource.Builder().setDataType(DataType.TYPE_SPEED).setType(DataSource.TYPE_DERIVED).build();
        static final DataSource powerDataSource =
            new DataSource.Builder().setDataType(DataType.TYPE_POWER_SAMPLE).setType(DataSource.TYPE_DERIVED).build();
        static final DataSource activitySegmentDataSource =
            new DataSource.Builder().setDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .setType(DataSource.TYPE_DERIVED)
                .build();

        GFClientWrapper subject;
        HistoryClient mockedHistoryClient;
        SessionsClient mockedSessionsClient;
        GFDataUtils gfDataUtilsSpy;

        public static boolean isCorrectDetailedSessionDeadRequest(SessionReadRequest request, Session session) {
            Date sessionStart = new Date(session.getStartTime(TimeUnit.MILLISECONDS));
            Date sessionEnd = new Date(session.getEndTime(TimeUnit.MILLISECONDS));
            Set<DataType> readingSessionDataTypes =
                Stream
                    .of(DataType.TYPE_STEP_COUNT_DELTA,
                        DataType.TYPE_HEART_RATE_BPM,
                        DataType.TYPE_SPEED,
                        DataType.TYPE_CALORIES_EXPENDED,
                        DataType.TYPE_POWER_SAMPLE,
                        DataType.TYPE_ACTIVITY_SEGMENT)
                    .collect(Collectors.toSet());
            return request.includeSessionsFromAllApps() && request.getSessionId().equals(session.getIdentifier())
                && isTimeIntervalOfRequest(request, sessionStart, sessionEnd)
                && request.getDataTypes().stream().collect(Collectors.toSet()).equals(readingSessionDataTypes);
        }

        public static boolean isSingleDataTypeReadRequest(DataReadRequest request, DataType type) {
            return request.getDataTypes().size() == 1 && request.getDataTypes().get(0).equals(type);
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
        public void getSessions_whenOnlyShortSessions_returnsTaskWithEmptyList()
            throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));

            Session shortSession = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setName("very short walking")
                .setStartTime(Date.from(Instant.parse("2020-10-01T10:00:00Z")).getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(Date.from(Instant.parse("2020-10-01T10:04:59Z")).getTime(), TimeUnit.MILLISECONDS)
                .build();
            SessionReadResponse readResponse = createTestSessionReadResponse(shortSession);
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(readResponse));

            // 5 minutes duration filter is more than the session duration
            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            assertTrue("should have empty list", result.getResult().isEmpty());

            InOrder inOrder = inOrder(mockedSessionsClient);
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, end) && request.includeSessionsFromAllApps();
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
        }

        @Test
        public void getSessions_whenOngoingSessions_returnsTaskWithEmptyList()
            throws ExecutionException, InterruptedException {
            Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));

            Session ongoingSession = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setName("ongoing walking")
                .setStartTime(Date.from(Instant.parse("2020-10-01T10:00:00Z")).getTime(), TimeUnit.MILLISECONDS)
                .build();
            SessionReadResponse readResponse = createTestSessionReadResponse(ongoingSession);
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(readResponse));

            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            assertTrue("should have empty list", result.getResult().isEmpty());

            InOrder inOrder = inOrder(mockedSessionsClient);
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, end) && request.includeSessionsFromAllApps();
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
        }

        @Test
        public void getSessions_whenOneCompletedSessionWithSuccessfulDetailedRequest_returnsTaskWithSessions()
            throws ExecutionException, InterruptedException {
            final Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));

            final Date sessionStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            final Date sessionEnd = Date.from(Instant.parse("2020-10-01T10:15:00Z"));

            Session _session = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setIdentifier("session#1")
                .setName("short walking")
                .setStartTime(sessionStart.getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                .build();
            // spied session to substitute package identifier
            Session session = spy(_session);
            when(session.getAppPackageName()).thenReturn("com.fitness.app");
            SessionReadResponse readResponse = createTestSessionReadResponse(session);
            DataSet hrDataSet = DataSet.builder(hrDataSource)
                .add(DataPoint.builder(hrDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_BPM, 77f)
                    .build())
                .build();
            DataSet caloriesDataSet = DataSet.builder(caloriesDataSource)
                .add(DataPoint.builder(caloriesDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_CALORIES, 13.2f)
                    .build())
                .build();
            DataSet stepsDataSet = DataSet.builder(stepsDataSource)
                .add(DataPoint.builder(stepsDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_STEPS, 1250)
                    .build())
                .build();
            DataSet powerDataSet = DataSet.builder(powerDataSource)
                .add(DataPoint.builder(powerDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_WATTS, 5f)
                    .build())
                .build();
            DataSet speedDataSet = DataSet.builder(speedDataSource)
                .add(DataPoint.builder(speedDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_SPEED, 0.87f)
                    .build())
                .build();
            DataSet activitySegmentsDataSet = DataSet.builder(activitySegmentDataSource)
                .add(DataPoint.builder(activitySegmentDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setActivityField(Field.FIELD_ACTIVITY, FitnessActivities.WALKING)
                    .build())
                .build();
            SessionReadResponse detailedSessionResponse = createTestSessionReadResponseWithDetailedSession(session,
                caloriesDataSet,
                hrDataSet,
                stepsDataSet,
                powerDataSet,
                speedDataSet,
                activitySegmentsDataSet);
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(readResponse))
                .thenReturn(Tasks.forResult(detailedSessionResponse));
            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            assertTrue("task should have a list with one session", result.getResult().size() == 1);
            GFSessionBundle sessionBundle = result.getResult().get(0);
            assertEquals("should collect the session bundle", "session#1", sessionBundle.getId());
            assertEquals("should collect the session bundle", "short walking", sessionBundle.getName());
            assertEquals("should collect the session bundle", "walking", sessionBundle.getActivityType());
            assertEquals("should collect the session bundle", 7, sessionBundle.getType());
            assertEquals("should collect the session bundle",
                "com.fitness.app",
                sessionBundle.getApplicationIdentifier());
            assertEquals("should collect the session bundle", sessionStart, sessionBundle.getTimeStart());
            assertEquals("should collect the session bundle", sessionEnd, sessionBundle.getTimeEnd());

            assertTrue("session bundle should have 1 calorie", sessionBundle.getCalories().size() == 1);
            GFCalorieDataPoint calorie = sessionBundle.getCalories().get(0);
            assertEquals("should collect calorie's start time", sessionStart, calorie.getStart());
            assertEquals("should collect calorie's end time", sessionEnd, calorie.getEnd());
            assertEquals("should collect calorie's kcals", 13.2f, calorie.getValue(), 0.00001);
            assertEquals("should collect calorie's datasource",
                "derived:com.google.calories.expended:",
                calorie.getDataSource());

            assertEquals("session bundle should have 1 steps", 1, sessionBundle.getSteps().size());
            GFStepsDataPoint steps = sessionBundle.getSteps().get(0);
            assertEquals("should collect steps' start time", sessionStart, steps.getStart());
            assertEquals("should collect steps' end time", sessionEnd, steps.getEnd());
            assertEquals("should collect steps count", (Integer) 1250, steps.getValue());
            assertEquals("should collect steps' datasource",
                "derived:com.google.step_count.delta:",
                steps.getDataSource());

            assertTrue("session bundle should have 1 hr", sessionBundle.getHeartRate().size() == 1);
            GFHRDataPoint hr = sessionBundle.getHeartRate().get(0);
            assertEquals("should collect hr's start time as an instant measurement", sessionEnd, hr.getStart());
            assertNull("should not collect hr's end time", hr.getEnd());
            assertEquals("should collect hr's bpm", 77f, hr.getValue(), 0.00001);
            assertEquals("should collect hr's datasource", "derived:com.google.heart_rate.bpm:", hr.getDataSource());

            assertTrue("session bundle should have 1 speed", sessionBundle.getSpeed().size() == 1);
            GFSpeedDataPoint speed = sessionBundle.getSpeed().get(0);
            assertEquals("should collect speed's start time as an instant measurement", sessionEnd, speed.getStart());
            assertNull("should not collect speed's end time", speed.getEnd());
            assertEquals("should collect speed's value", 0.87f, speed.getValue(), 0.00001);
            assertEquals("should collect speed's datasource", "derived:com.google.speed:", speed.getDataSource());

            assertTrue("session bundle should have 1 power sample", sessionBundle.getPower().size() == 1);
            GFPowerDataPoint power = sessionBundle.getPower().get(0);
            assertEquals("should collect power's start time as an instant measurement", sessionEnd, power.getStart());
            assertNull("should not collect power's end time", power.getEnd());
            assertEquals("should collect power's watts", 5f, power.getValue(), 0.00001);
            assertEquals("should collect power's datasource",
                "derived:com.google.power.sample:",
                power.getDataSource());

            assertTrue("session bundle should have 1 activity segment",
                sessionBundle.getActivitySegments().size() == 1);
            GFActivitySegmentDataPoint activitySegment = sessionBundle.getActivitySegments().get(0);
            assertEquals("should collect segment's start time", sessionStart, activitySegment.getStart());
            assertEquals("should collect segment's end time", sessionEnd, activitySegment.getEnd());
            assertEquals("should collect segment's type", (Integer) 7, activitySegment.getValue());
            assertEquals("should collect segment's datasource",
                "derived:com.google.activity.segment:",
                activitySegment.getDataSource());

            InOrder inOrder = inOrder(mockedSessionsClient);
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, end) && request.includeSessionsFromAllApps();
            }));
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isCorrectDetailedSessionDeadRequest(request, session);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
        }

        @Test
        public void getSessions_whenOneCompletedSessionWithFailedDetailedRequest_returnsTaskWithSessions()
            throws ExecutionException, InterruptedException {
            final Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));

            final Date sessionStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            final Date sessionEnd = Date.from(Instant.parse("2020-10-01T10:15:00Z"));

            Session _session = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setIdentifier("session#1")
                .setName("short walking")
                .setStartTime(sessionStart.getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                .build();
            // spied session to substitute package identifier
            Session session = spy(_session);
            when(session.getAppPackageName()).thenReturn("com.fitness.app");
            SessionReadResponse readResponse = createTestSessionReadResponse(session);
            DataSet hrDataSet = DataSet.builder(hrDataSource)
                .add(DataPoint.builder(hrDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_BPM, 77f)
                    .build())
                .build();
            DataSet caloriesDataSet = DataSet.builder(caloriesDataSource)
                .add(DataPoint.builder(caloriesDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_CALORIES, 13.2f)
                    .build())
                .build();
            DataSet stepsDataSet = DataSet.builder(stepsDataSource)
                .add(DataPoint.builder(stepsDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_STEPS, 1250)
                    .build())
                .build();
            DataSet powerDataSet = DataSet.builder(powerDataSource)
                .add(DataPoint.builder(powerDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_WATTS, 5f)
                    .build())
                .build();
            DataSet speedDataSet = DataSet.builder(speedDataSource)
                .add(DataPoint.builder(speedDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setField(Field.FIELD_SPEED, 0.87f)
                    .build())
                .build();
            DataSet activitySegmentsDataSet = DataSet.builder(activitySegmentDataSource)
                .add(DataPoint.builder(activitySegmentDataSource)
                    .setTimeInterval(sessionStart.getTime(), sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                    .setActivityField(Field.FIELD_ACTIVITY, FitnessActivities.WALKING)
                    .build())
                .build();
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(readResponse))
                .thenReturn(Tasks.forResult(null).continueWithTask(testUtilExecutor, (t) -> {
                    Thread.sleep(5000);
                    return Tasks.forCanceled();
                }));
            when(mockedHistoryClient.readData(Mockito.any())).thenAnswer(invocation -> {
                DataReadRequest request = invocation.getArgument(0, DataReadRequest.class);
                if (request.getDataTypes().size() != 1) {
                    return null;
                }
                DataType type = request.getDataTypes().get(0);
                if (DataType.TYPE_CALORIES_EXPENDED.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(caloriesDataSet));
                } else if (DataType.TYPE_STEP_COUNT_DELTA.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(stepsDataSet));
                } else if (DataType.TYPE_SPEED.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(speedDataSet));
                } else if (DataType.TYPE_POWER_SAMPLE.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(powerDataSet));
                } else if (DataType.TYPE_ACTIVITY_SEGMENT.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(activitySegmentsDataSet));
                } else if (DataType.TYPE_HEART_RATE_BPM.equals(type)) {
                    return Tasks.forResult(createTestDataReadResponse(hrDataSet));
                }
                throw new IllegalArgumentException("Unpredictable invocation");
            });
            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            assertTrue("task should have a list with one session", result.getResult().size() == 1);
            GFSessionBundle sessionBundle = result.getResult().get(0);
            assertEquals("should collect the session bundle", "session#1", sessionBundle.getId());
            assertEquals("should collect the session bundle", "short walking", sessionBundle.getName());
            assertEquals("should collect the session bundle", "walking", sessionBundle.getActivityType());
            assertEquals("should collect the session bundle", 7, sessionBundle.getType());
            assertEquals("should collect the session bundle",
                "com.fitness.app",
                sessionBundle.getApplicationIdentifier());
            assertEquals("should collect the session bundle", sessionStart, sessionBundle.getTimeStart());
            assertEquals("should collect the session bundle", sessionEnd, sessionBundle.getTimeEnd());

            assertTrue("session bundle should have 1 calorie", sessionBundle.getCalories().size() == 1);
            GFCalorieDataPoint calorie = sessionBundle.getCalories().get(0);
            assertEquals("should collect calorie's start time", sessionStart, calorie.getStart());
            assertEquals("should collect calorie's end time", sessionEnd, calorie.getEnd());
            assertEquals("should collect calorie's kcals", 13.2f, calorie.getValue(), 0.00001);
            assertEquals("should collect calorie's datasource",
                "derived:com.google.calories.expended:",
                calorie.getDataSource());

            assertTrue("session bundle should have 1 steps", sessionBundle.getSteps().size() == 1);
            GFStepsDataPoint steps = sessionBundle.getSteps().get(0);
            assertEquals("should collect steps' start time", sessionStart, steps.getStart());
            assertEquals("should collect steps' end time", sessionEnd, steps.getEnd());
            assertEquals("should collect steps count", (Integer) 1250, steps.getValue());
            assertEquals("should collect steps' datasource",
                "derived:com.google.step_count.delta:",
                steps.getDataSource());

            assertTrue("session bundle should have 1 hr", sessionBundle.getHeartRate().size() == 1);
            GFHRDataPoint hr = sessionBundle.getHeartRate().get(0);
            assertEquals("should collect hr's start time as an instant measurement", sessionEnd, hr.getStart());
            assertNull("should not collect hr's end time", hr.getEnd());
            assertEquals("should collect hr's bpm", 77f, hr.getValue(), 0.00001);
            assertEquals("should collect hr's datasource", "derived:com.google.heart_rate.bpm:", hr.getDataSource());

            assertTrue("session bundle should have 1 speed", sessionBundle.getSpeed().size() == 1);
            GFSpeedDataPoint speed = sessionBundle.getSpeed().get(0);
            assertEquals("should collect speed's start time as an instant measurement", sessionEnd, speed.getStart());
            assertNull("should not collect speed's end time", speed.getEnd());
            assertEquals("should collect speed's value", 0.87f, speed.getValue(), 0.00001);
            assertEquals("should collect speed's datasource", "derived:com.google.speed:", speed.getDataSource());

            assertTrue("session bundle should have 1 power sample", sessionBundle.getPower().size() == 1);
            GFPowerDataPoint power = sessionBundle.getPower().get(0);
            assertEquals("should collect power's start time as an instant measurement", sessionEnd, power.getStart());
            assertNull("should not collect power's end time", power.getEnd());
            assertEquals("should collect power's watts", 5f, power.getValue(), 0.00001);
            assertEquals("should collect power's datasource",
                "derived:com.google.power.sample:",
                power.getDataSource());

            assertTrue("session bundle should have 1 activity segment",
                sessionBundle.getActivitySegments().size() == 1);
            GFActivitySegmentDataPoint activitySegment = sessionBundle.getActivitySegments().get(0);
            assertEquals("should collect segment's start time", sessionStart, activitySegment.getStart());
            assertEquals("should collect segment's end time", sessionEnd, activitySegment.getEnd());
            assertEquals("should collect segment's type", (Integer) 7, activitySegment.getValue());
            assertEquals("should collect segment's datasource",
                "derived:com.google.activity.segment:",
                activitySegment.getDataSource());

            InOrder sessionClientInOrder = inOrder(mockedSessionsClient);
            sessionClientInOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, end) && request.includeSessionsFromAllApps();
            }));
            sessionClientInOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isCorrectDetailedSessionDeadRequest(request, session);
            }));
            sessionClientInOrder.verifyNoMoreInteractions();
            InOrder historyClientInOrder = inOrder(mockedHistoryClient);
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_HEART_RATE_BPM)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_STEP_COUNT_DELTA)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_SPEED)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_POWER_SAMPLE)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_CALORIES_EXPENDED)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_ACTIVITY_SEGMENT)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));
            historyClientInOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
            // should split date ranges into 15-minute chunks for subqueries
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(sessionStart, sessionEnd, Duration.ofMinutes(15));
        }


        @Test
        public void getSessions_whenTwoSessionLists_returnsTaskWithSessions()
            throws ExecutionException, InterruptedException {
            final Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-10-08T23:59:59.999Z"));

            final Date sessionStart1 = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            final Date sessionEnd1 = Date.from(Instant.parse("2020-10-01T10:15:00Z"));
            final Session _session1 = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setIdentifier("session#1")
                .setName("short walking")
                .setStartTime(sessionStart1.getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(sessionEnd1.getTime(), TimeUnit.MILLISECONDS)
                .build();
            // spied session to substitute package identifier
            final Session session1 = spy(_session1);
            when(session1.getAppPackageName()).thenReturn("com.fitness.app");

            final Date sessionStart2 = Date.from(Instant.parse("2020-10-07T17:05:00Z"));
            final Date sessionEnd2 = Date.from(Instant.parse("2020-10-07T17:25:00Z"));
            final Session _session2 = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setIdentifier("session#2")
                .setName("running")
                .setStartTime(sessionStart2.getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(sessionEnd2.getTime(), TimeUnit.MILLISECONDS)
                .build();
            // spied session to substitute package identifier
            final Session session2 = spy(_session2);
            when(session2.getAppPackageName()).thenReturn("com.fitness.app");

            SessionReadResponse sessionResponse1 = createTestSessionReadResponse(session1);
            SessionReadResponse sessionResponse2 = createTestSessionReadResponse(session2);
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(sessionResponse1))
                .thenReturn(Tasks.forResult(sessionResponse2))
                .thenReturn(Tasks.forResult(sessionResponse1))
                .thenReturn(Tasks.forResult(sessionResponse2));

            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            testExecutor.submit(() -> Tasks.await(result)).get();
            assertTrue("successful task", result.isSuccessful());
            List<GFSessionBundle> sessionBundles = result.getResult();
            assertEquals("task should have a list with 2 sessions", 2, sessionBundles.size());
            assertEquals("should return sessions in the order of responses",
                "session#1",
                sessionBundles.get(0).getId());
            assertEquals("should return sessions in the order of responses",
                "session#2",
                sessionBundles.get(1).getId());

            InOrder inOrder = inOrder(mockedSessionsClient);
            // should first request session lists for the specified dates
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, Date.from(Instant.parse("2020-10-06T00:00:00Z")))
                    && request.includeSessionsFromAllApps();
            }));
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, Date.from(Instant.parse("2020-10-06T00:00:00Z")), end)
                    && request.includeSessionsFromAllApps();
            }));
            // // should then request detailed sessions one be one
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isCorrectDetailedSessionDeadRequest(request, session1);
            }));
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isCorrectDetailedSessionDeadRequest(request, session2);
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
        }

        @Test
        public void getSessions_whenTwoSessionListsButTheFirstRequestDies_returnsFailedTask()
            throws InterruptedException {
            final Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-10-08T23:59:59.999Z"));

            when(mockedSessionsClient.readSession(Mockito.any()))
                // simulate the dead request to gf service
                .thenReturn(Tasks.forResult(null).continueWithTask(testUtilExecutor, (t) -> {
                    Thread.sleep(5000);
                    return Tasks.forCanceled();
                }));

            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) {
                /* catch expected ExecutionException */ }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException triesCountExceededException = (MaxTriesCountExceededException) exception;
            assertThat("exception should have error message",
                triesCountExceededException.getMessage(),
                startsWith("Possible tries count (1) exceeded for task \"'fetch gf sessions' for 2020-10-01"));

            InOrder inOrder = inOrder(mockedSessionsClient);
            // should only try to request the first session list
            inOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, Date.from(Instant.parse("2020-10-06T00:00:00Z")))
                    && request.includeSessionsFromAllApps();
            }));
            inOrder.verifyNoMoreInteractions();

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
        }

        @Test
        public void getSessions_whenOneSessionListsButTheDetailedRequestAndDataReadRequestsDie_returnsFailedTask()
            throws InterruptedException {
            final Date start = Date.from(Instant.parse("2020-10-01T00:00:00Z"));
            final Date end = Date.from(Instant.parse("2020-10-01T23:59:59.999Z"));

            final Date sessionStart = Date.from(Instant.parse("2020-10-01T10:00:00Z"));
            final Date sessionEnd = Date.from(Instant.parse("2020-10-01T10:15:00Z"));
            final Session _session = new Session.Builder().setActivity(FitnessActivities.WALKING)
                .setIdentifier("session#1")
                .setName("short walking")
                .setStartTime(sessionStart.getTime(), TimeUnit.MILLISECONDS)
                .setEndTime(sessionEnd.getTime(), TimeUnit.MILLISECONDS)
                .build();
            // spied session to substitute package identifier
            final Session session = spy(_session);
            when(session.getAppPackageName()).thenReturn("com.fitness.app");

            SessionReadResponse readResponse = createTestSessionReadResponseWithDetailedSession(session);
            when(mockedSessionsClient.readSession(Mockito.any())).thenReturn(Tasks.forResult(readResponse))
                // simulate the dead request to gf service
                .thenReturn(Tasks.forResult(null).continueWithTask(testUtilExecutor, (t) -> {
                    Thread.sleep(5000);
                    return Tasks.forCanceled();
                }));
            when(mockedHistoryClient.readData(Mockito.any()))
                // simulate the dead request to gf service
                .thenReturn(Tasks.forResult(null).continueWithTask(testUtilExecutor, (t) -> {
                    Thread.sleep(5000);
                    return Tasks.forCanceled();
                }));

            Task<List<GFSessionBundle>> result = subject.getSessions(start, end, Duration.ofMinutes(5));
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) {
                /* catch expected ExecutionException */ }

            assertFalse("unsuccessful task", result.isSuccessful());
            Exception exception = result.getException();
            assertThat(exception, instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException triesCountExceededException = (MaxTriesCountExceededException) exception;
            assertThat("exception should have error message",
                triesCountExceededException.getMessage(),
                startsWith(
                    "Possible tries count (1) exceeded for task \"'fetch gf HR for session session#1' for 2020-10-01"));

            InOrder sessionClientInOrder = inOrder(mockedSessionsClient);
            sessionClientInOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isTimeIntervalOfRequest(request, start, end) && request.includeSessionsFromAllApps();
            }));
            sessionClientInOrder.verify(mockedSessionsClient).readSession(argThat(request -> {
                return isCorrectDetailedSessionDeadRequest(request, session);
            }));
            sessionClientInOrder.verifyNoMoreInteractions();

            InOrder historyClientInOrder = inOrder(mockedHistoryClient);
            historyClientInOrder.verify(mockedHistoryClient).readData(argThat(request -> {
                return isSingleDataTypeReadRequest(request, DataType.TYPE_HEART_RATE_BPM)
                    && isTimeIntervalOfRequest(request, sessionStart, sessionEnd);
            }));

            // should split input date ranges into 5-day chunks for the session list
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(start, end, Duration.ofDays(5));
            // should split date ranges into 15-minute chunks for subqueries
            verify(gfDataUtilsSpy).splitDateRangeIntoChunks(sessionStart, sessionEnd, Duration.ofMinutes(15));
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

    public static DataPoint createAggregatedHRDataPoint(DataSource source,
        Date start,
        Date end,
        float min,
        float max,
        float avg) {
        return DataPoint.builder(source)
            .setTimeInterval(start.getTime(), end.getTime(), TimeUnit.MILLISECONDS)
            .setField(Field.FIELD_MIN, min)
            .setField(Field.FIELD_MAX, max)
            .setField(Field.FIELD_AVERAGE, avg)
            .build();
    }

    public static DataReadResponse createTestDataReadResponse(Bucket... buckets) {
        DataReadResponse dataReadResponse = new DataReadResponse();
        DataReadResult mockedDataReadResult = mock(DataReadResult.class);
        when(mockedDataReadResult.getBuckets()).thenReturn(Arrays.asList(buckets));
        dataReadResponse.setResult(mockedDataReadResult);
        return dataReadResponse;
    }

    public static DataReadResponse createTestDataReadResponse(DataSet... dataSets) {
        DataReadResponse dataReadResponse = new DataReadResponse();
        DataReadResult mockedDataReadResult = mock(DataReadResult.class);
        Arrays.stream(dataSets).forEach(dataSet -> {
            when(mockedDataReadResult.getDataSet(dataSet.getDataType())).thenReturn(dataSet);
        });
        dataReadResponse.setResult(mockedDataReadResult);
        return dataReadResponse;
    }

    public static SessionReadResponse createTestSessionReadResponse(Session... sessions) {
        SessionReadResponse sessionReadResponse = new SessionReadResponse();
        SessionReadResult mockedReadResult = mock(SessionReadResult.class);
        when(mockedReadResult.getSessions()).thenReturn(Arrays.asList(sessions));
        sessionReadResponse.setResult(mockedReadResult);
        return sessionReadResponse;
    }

    public static SessionReadResponse createTestSessionReadResponseWithDetailedSession(Session session,
        DataSet... dataSets) {
        SessionReadResponse sessionReadResponse = new SessionReadResponse();
        SessionReadResult mockedReadResult = mock(SessionReadResult.class);
        when(mockedReadResult.getSessions()).thenReturn(Arrays.asList(session));
        when(mockedReadResult.getDataSet(session)).thenReturn(Arrays.asList(dataSets));
        sessionReadResponse.setResult(mockedReadResult);
        return sessionReadResponse;
    }
}
