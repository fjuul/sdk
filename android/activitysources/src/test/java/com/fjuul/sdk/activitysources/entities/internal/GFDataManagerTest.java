package com.fjuul.sdk.activitysources.entities.internal;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions;
import com.fjuul.sdk.activitysources.entities.GoogleFitSessionSyncOptions;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFDataPointsBatch;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHRSummaryDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFHeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFSessionBundle;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFStepsDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFWeightDataPoint;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.sync_metadata.GFSyncMetadataStore;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.test.LoggableTestSuite;
import com.fjuul.sdk.test.utils.TimberLogEntry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import android.os.Build;
import android.util.Log;

@RunWith(Enclosed.class)
public class GFDataManagerTest {
    public static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void shutdownExecutor() {
        testExecutor.shutdownNow();
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    abstract static class GivenRobolectricContext extends LoggableTestSuite {}

    public static class SyncIntradayMetricsTest extends GivenRobolectricContext {
        final String currentInstant = "2020-10-05T09:30:00Z";
        final TimeZone testTimeZone = TimeZone.getTimeZone("Europe/Zurich");
        final ZoneId testZoneId = testTimeZone.toZoneId();
        final Clock fixedClock = Clock.fixed(Instant.parse(currentInstant), testZoneId);
        final Date lowerDateBoundary = Date.from(LocalDateTime.parse("2020-09-28T15:40:00").atZone(testZoneId).toInstant());

        GFDataManager subject;
        GFClientWrapper mockedGFClientWrapper;
        GFDataUtils gfDataUtilsSpy;
        GFSyncMetadataStore mockedGFSyncMetadataStore;
        ActivitySourcesService mockedActivitySourcesService;

        @Before
        public void beforeTests() {
            mockedGFClientWrapper = mock(GFClientWrapper.class);
            mockedGFSyncMetadataStore = mock(GFSyncMetadataStore.class);
            mockedActivitySourcesService = mock(ActivitySourcesService.class);
            GFDataUtils gfDataUtils = new GFDataUtils(testZoneId, fixedClock);
            gfDataUtilsSpy = spy(gfDataUtils);
            subject = new GFDataManager(mockedGFClientWrapper,
                gfDataUtilsSpy,
                mockedGFSyncMetadataStore,
                mockedActivitySourcesService,
                lowerDateBoundary);
        }

        @Test
        public void syncIntradayMetrics_StartDateAndEndDateCrossTheLowerDateBoundary_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-09-20");
            final LocalDate endDate = LocalDate.parse("2020-09-25");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // shouldn't interact with client-wrapper
            verifyNoInteractions(mockedGFClientWrapper);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entry", 1, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: skip syncing GF intraday metrics (INTRADAY_CALORIES) with input dates [2020-09-20, 2020-09-25]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_whenStartDateCrossesTheLowerDateBoundary_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-09-20");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate = lowerDateBoundary;
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            List<GFCalorieDataPoint> calories = Stream.of(new GFCalorieDataPoint(10f,
                Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                "derived:com.google.calories.expended:Brand:tracker")).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(false);

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-09-28T15:30:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30));
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(caloriesBatch -> {
                assertEquals("calories batch should have data points", calories, caloriesBatch.getPoints());
                assertEquals("calories batch should have right start time",
                    Date.from(Instant.parse("2020-10-01T09:00:00Z")),
                    caloriesBatch.getStartTime());
                assertEquals("calories batch should have right end time",
                    Date.from(Instant.parse("2020-10-01T09:30:00Z")),
                    caloriesBatch.getEndTime());
                return true;
            }));
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_CALORIES) with date range [2020-09-28T13:40:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_emptyGFDataResponseForCalories_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(Collections.emptyList()));

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                Collections.emptyList(),
                Duration.ofMinutes(30));
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_CALORIES) with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_failedGFRequestForSteps_returnsFailedTask() throws InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_STEPS)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            when(mockedGFClientWrapper.getSteps(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forException(new MaxTriesCountExceededException("failed")));

            Task<Void> result = subject.syncIntradayMetrics(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) {
                /* expected exception */ }

            assertFalse("should return failed task", result.isSuccessful());
            assertThat(result.getException(), instanceOf(MaxTriesCountExceededException.class));
            MaxTriesCountExceededException exception = (MaxTriesCountExceededException) result.getException();
            assertEquals("exception should have message", "failed", exception.getMessage());

            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSteps(startRequestDate, endRequestDate);
            // should not more invoke gf data utils
            verify(gfDataUtilsSpy).adjustInputDatesForGFRequest(startDate, endDate);
            verifyNoMoreInteractions(gfDataUtilsSpy);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entry", 1, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_STEPS) with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_alreadySyncedGFDataResponseForCalories_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            List<GFCalorieDataPoint> calories = Stream.of(new GFCalorieDataPoint(10f,
                Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                "derived:com.google.calories.expended:Brand:tracker")).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(false);

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30));
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(caloriesBatch -> {
                assertEquals("calories batch should have data points", calories, caloriesBatch.getPoints());
                assertEquals("calories batch should have right start time",
                    Date.from(Instant.parse("2020-10-01T09:00:00Z")),
                    caloriesBatch.getStartTime());
                assertEquals("calories batch should have right end time",
                    Date.from(Instant.parse("2020-10-01T09:30:00Z")),
                    caloriesBatch.getEndTime());
                return true;
            }));
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_CALORIES) with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_notSyncedGFDataResponseForCaloriesWithFailedApiRequest_returnsFailedTask()
            throws InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            List<GFCalorieDataPoint> calories = Stream.of(new GFCalorieDataPoint(10f,
                Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                "derived:com.google.calories.expended:Brand:tracker")).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(true);


            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            final ApiExceptions.BadRequestException apiCallException =
                new ApiExceptions.BadRequestException("Bad request");
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.error(apiCallException));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncIntradayMetrics(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) {
                /* expected exception */ }

            assertFalse("should return failed task", result.isSuccessful());
            assertThat(result.getException(), instanceOf(CommonException.class));
            CommonException exception = (CommonException) result.getException();
            assertEquals("should have exception message", "Failed to send data to the server", exception.getMessage());
            assertEquals("should have exception cause", apiCallException, exception.getCause());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30));
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(caloriesBatch -> {
                assertEquals("calories batch should have data points", calories, caloriesBatch.getPoints());
                assertEquals("calories batch should have right start time",
                    Date.from(Instant.parse("2020-10-01T09:00:00Z")),
                    caloriesBatch.getStartTime());
                assertEquals("calories batch should have right end time",
                    Date.from(Instant.parse("2020-10-01T09:30:00Z")),
                    caloriesBatch.getEndTime());
                return true;
            }));
            // should not store the metadata
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);
            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should send the calories data", uploadData.getCaloriesData(), calories);
                return true;
            }));

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_CALORIES) with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending new GF data: GFUploadData{calories=1, steps=0, heartRates=0, sessions=0}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: failed to send GF data: Bad request", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_notSyncedGFDataResponseForCaloriesWithSuccessfulApiRequest_returnsSuccessfulTask()
            throws InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            List<GFCalorieDataPoint> calories = Stream.of(new GFCalorieDataPoint(10f,
                Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                "derived:com.google.calories.expended:Brand:tracker")).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(true);


            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncIntradayMetrics(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) {
                /* expected exception */ }

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30));
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);

            // should ask the metadata store about the calories batch
            ArgumentCaptor<GFDataPointsBatch<GFCalorieDataPoint>> caloriesBatchCaptor =
                ArgumentCaptor.forClass(GFDataPointsBatch.class);
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(caloriesBatchCaptor.capture());
            GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = caloriesBatchCaptor.getValue();
            assertEquals("calories batch should have data points", calories, caloriesBatch.getPoints());
            assertEquals("calories batch should have right start time",
                Date.from(Instant.parse("2020-10-01T09:00:00Z")),
                caloriesBatch.getStartTime());
            assertEquals("calories batch should have right end time",
                Date.from(Instant.parse("2020-10-01T09:30:00Z")),
                caloriesBatch.getEndTime());
            // should ask the metadata store to save the metadata
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfCalories(caloriesBatch);
            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should send the calories data", uploadData.getCaloriesData(), calories);
                return true;
            }));

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: start syncing GF intraday metrics (INTRADAY_CALORIES) with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending new GF data: GFUploadData{calories=1, steps=0, heartRates=0, sessions=0}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: succeeded to send GF data", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncIntradayMetrics_notSyncedGFDataResponseForAllMetricsWithSuccessfulApiRequest_returnsSuccessfulTask()
            throws InterruptedException, ExecutionException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GoogleFitIntradaySyncOptions options =
                new GoogleFitIntradaySyncOptions.Builder(fixedClock).include(FitnessMetricsType.INTRADAY_CALORIES)
                    .include(FitnessMetricsType.INTRADAY_HEART_RATE)
                    .include(FitnessMetricsType.INTRADAY_STEPS)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            List<GFCalorieDataPoint> calories = Stream.of(new GFCalorieDataPoint(10f,
                Date.from(LocalDateTime.parse("2020-10-01T09:01:00").atZone(testZoneId).toInstant()),
                "derived:com.google.calories.expended:Brand:tracker")).collect(Collectors.toList());
            List<GFStepsDataPoint> steps = Stream.of(new GFStepsDataPoint(185,
                Date.from(LocalDateTime.parse("2020-10-01T09:01:00").atZone(testZoneId).toInstant()),
                "derived:com.google.step_count.delta:Brand:tracker")).collect(Collectors.toList());
            List<GFHRSummaryDataPoint> hr = Stream.of(new GFHRSummaryDataPoint(70f,
                69f,
                71f,
                Date.from(LocalDateTime.parse("2020-10-01T09:01:00").atZone(testZoneId).toInstant()),
                "derived:com.google.heart_rate.summary:Brand:tracker")).collect(Collectors.toList());

            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(calories));
            when(mockedGFClientWrapper.getSteps(startRequestDate, endRequestDate)).thenReturn(Tasks.forResult(steps));
            when(mockedGFClientWrapper.getHRSummaries(startRequestDate, endRequestDate))
                .thenReturn(Tasks.forResult(hr));

            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(true);
            when(mockedGFSyncMetadataStore.isNeededToSyncStepsBatch(any())).thenReturn(true);
            when(mockedGFSyncMetadataStore.isNeededToSyncHRBatch(any())).thenReturn(true);

            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());

            // should adjust input dates for calories and hr batching
            verify(gfDataUtilsSpy, times(2)).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofMinutes(30));
            // should adjust input dates for the steps batching
            verify(gfDataUtilsSpy).roundDatesByIntradayBatchDuration(startRequestDate, endRequestDate, Duration.ofHours(6));

            // should split the gf calories into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30));
            // should split the gf steps into 6-hours batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                steps,
                Duration.ofHours(6));
            // should split the gf hr into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                hr,
                Duration.ofMinutes(30));

            // should ask client-wrapper for calories data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask client-wrapper for steps data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSteps(startRequestDate, endRequestDate);
            // should ask client-wrapper for steps data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getHRSummaries(startRequestDate, endRequestDate);

            // should ask the metadata store about the calories batch
            ArgumentCaptor<GFDataPointsBatch<GFCalorieDataPoint>> caloriesBatchCaptor =
                ArgumentCaptor.forClass(GFDataPointsBatch.class);
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(caloriesBatchCaptor.capture());
            GFDataPointsBatch<GFCalorieDataPoint> caloriesBatch = caloriesBatchCaptor.getValue();
            assertEquals("calories batch should have data points", caloriesBatch.getPoints(), calories);
            assertEquals("calories batch should have right start time",
                Date.from(LocalDateTime.parse("2020-10-01T09:00:00").atZone(testZoneId).toInstant()),
                caloriesBatch.getStartTime());
            assertEquals("calories batch should have right end time",
                Date.from(LocalDateTime.parse("2020-10-01T09:30:00").atZone(testZoneId).toInstant()),
                caloriesBatch.getEndTime());

            // should ask the metadata store about the steps batch
            ArgumentCaptor<GFDataPointsBatch<GFStepsDataPoint>> stepsBatchCaptor =
                ArgumentCaptor.forClass(GFDataPointsBatch.class);
            verify(mockedGFSyncMetadataStore).isNeededToSyncStepsBatch(stepsBatchCaptor.capture());
            GFDataPointsBatch<GFStepsDataPoint> stepsBatch = stepsBatchCaptor.getValue();
            assertEquals("steps batch should have data points", steps, stepsBatch.getPoints());
            assertEquals("steps batch should have right start time",
                Date.from(LocalDateTime.parse("2020-10-01T06:00:00").atZone(testZoneId).toInstant()),
                stepsBatch.getStartTime());
            assertEquals("steps batch should have right end time",
                Date.from(LocalDateTime.parse("2020-10-01T12:00:00").atZone(testZoneId).toInstant()),
                stepsBatch.getEndTime());

            // should ask the metadata store about the hr batch
            ArgumentCaptor<GFDataPointsBatch<GFHRSummaryDataPoint>> hrBatchCaptor =
                ArgumentCaptor.forClass(GFDataPointsBatch.class);
            verify(mockedGFSyncMetadataStore).isNeededToSyncHRBatch(hrBatchCaptor.capture());
            GFDataPointsBatch<GFHRSummaryDataPoint> hrBatch = hrBatchCaptor.getValue();
            assertEquals("hr batch should have data points", hrBatch.getPoints(), hr);
            assertEquals("hr batch should have right start time",
                Date.from(LocalDateTime.parse("2020-10-01T09:00:00").atZone(testZoneId).toInstant()),
                hrBatch.getStartTime());
            assertEquals("hr batch should have right end time",
                Date.from(LocalDateTime.parse("2020-10-01T09:30:00").atZone(testZoneId).toInstant()),
                hrBatch.getEndTime());

            // should ask the metadata store to save the metadata of calories batch
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfCalories(caloriesBatch);
            // should ask the metadata store to save the metadata of steps batch
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfSteps(stepsBatch);
            // should ask the metadata store to save the metadata of hr batch
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfHR(hrBatch);

            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should send the calories data", calories, uploadData.getCaloriesData());
                assertEquals("should send the steps data", steps, uploadData.getStepsData());
                assertEquals("should send the hr data", hr, uploadData.getHrData());
                return true;
            }));

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertThat(logEntry.getMessage(), containsString("INTRADAY_STEPS"));
            assertThat(logEntry.getMessage(), containsString("INTRADAY_HEART_RATE"));
            assertThat(logEntry.getMessage(), containsString("INTRADAY_CALORIES"));
            assertThat(logEntry.getMessage(), containsString("with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]"));
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending new GF data: GFUploadData{calories=1, steps=1, heartRates=1, sessions=0}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: succeeded to send GF data", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }
    }

    public static class SyncSessions extends GivenRobolectricContext {
        final String currentInstant = "2020-10-05T09:30:00Z";
        final TimeZone testTimeZone = TimeZone.getTimeZone("Europe/Zurich");
        final ZoneId testZoneId = testTimeZone.toZoneId();
        final Clock fixedClock = Clock.fixed(Instant.parse(currentInstant), testZoneId);
        final Date lowerDateBoundary = Date.from(LocalDateTime.parse("2020-09-28T15:40:00").atZone(testZoneId).toInstant());

        GFDataManager subject;
        GFClientWrapper mockedGFClientWrapper;
        GFDataUtils gfDataUtilsSpy;
        GFSyncMetadataStore mockedGFSyncMetadataStore;
        ActivitySourcesService mockedActivitySourcesService;

        @Before
        public void beforeTests() {
            mockedGFClientWrapper = mock(GFClientWrapper.class);
            mockedGFSyncMetadataStore = mock(GFSyncMetadataStore.class);
            mockedActivitySourcesService = mock(ActivitySourcesService.class);
            GFDataUtils gfDataUtils = new GFDataUtils(testZoneId, fixedClock);
            gfDataUtilsSpy = spy(gfDataUtils);
            subject = new GFDataManager(mockedGFClientWrapper,
                gfDataUtilsSpy,
                mockedGFSyncMetadataStore,
                mockedActivitySourcesService,
                lowerDateBoundary);
        }

        @Test
        public void syncSessions_startDateAndEndDateCrossTheLowerDateBoundary_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-09-20");
            final LocalDate endDate = LocalDate.parse("2020-09-25");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            Task<Void> result = subject.syncSessions(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // shouldn't ask the client-wrapper
            verifyNoInteractions(mockedGFClientWrapper);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entry", 1, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: skip syncing GF sessions with input dates [2020-09-20, 2020-09-25]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncSessions_startDateCrossesTheLowerDateBoundary_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-09-20");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate = lowerDateBoundary;
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            when(mockedGFClientWrapper.getSessions(startRequestDate, endRequestDate, minDuration))
                .thenReturn(Tasks.forResult(Collections.emptyList()));

            Task<Void> result = subject.syncSessions(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSessions(startRequestDate, endRequestDate, minDuration);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF sessions with date range [2020-09-28T13:40:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncSessions_emptyGFResponse_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            when(mockedGFClientWrapper.getSessions(startRequestDate, endRequestDate, minDuration))
                .thenReturn(Tasks.forResult(Collections.emptyList()));

            Task<Void> result = subject.syncSessions(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSessions(startRequestDate, endRequestDate, minDuration);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF sessions with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncSessions_whenAlreadySyncedSession_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            final GFSessionBundle stubSessionBundle = mock(GFSessionBundle.class);

            when(mockedGFClientWrapper.getSessions(startRequestDate, endRequestDate, minDuration))
                .thenReturn(Tasks.forResult(Arrays.asList(stubSessionBundle)));
            when(mockedGFSyncMetadataStore.isNeededToSyncSessionBundle(stubSessionBundle)).thenReturn(false);

            Task<Void> result = subject.syncSessions(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSessions(startRequestDate, endRequestDate, minDuration);
            // should ask the sync metadata store about session
            verify(mockedGFSyncMetadataStore).isNeededToSyncSessionBundle(stubSessionBundle);
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF sessions with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no new data to send", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncSessions_whenNotSyncedSessionWithFailedApiRequest_returnsSuccessfulTask()
            throws InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            final GFSessionBundle stubSessionBundle = mock(GFSessionBundle.class);

            when(mockedGFClientWrapper.getSessions(startRequestDate, endRequestDate, minDuration))
                .thenReturn(Tasks.forResult(Arrays.asList(stubSessionBundle)));
            when(mockedGFSyncMetadataStore.isNeededToSyncSessionBundle(stubSessionBundle)).thenReturn(true);
            final ApiCall mockedApiCall = mock(ApiCall.class);
            final ApiExceptions.BadRequestException requestException =
                new ApiExceptions.BadRequestException("Bad request");
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.error(requestException));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncSessions(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exc) {
                /* expected exception */ }

            assertFalse("should return unsuccessful task", result.isSuccessful());
            assertThat(result.getException(), instanceOf(CommonException.class));
            CommonException exception = (CommonException) result.getException();
            assertEquals("exception should have message", "Failed to send data to the server", exception.getMessage());
            assertEquals("exception should carry original cause", requestException, exception.getCause());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSessions(startRequestDate, endRequestDate, minDuration);
            // should ask the sync metadata store about session
            verify(mockedGFSyncMetadataStore).isNeededToSyncSessionBundle(stubSessionBundle);
            // should not store the metadata
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);
            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should try to send the sessions data",
                    Arrays.asList(stubSessionBundle),
                    uploadData.getSessionsData());
                return true;
            }));

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF sessions with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending new GF data: GFUploadData{calories=0, steps=0, heartRates=0, sessions=1}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: failed to send GF data: Bad request", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncSessions_whenNotSyncedSessionWithSuccessfulApiRequest_returnsSuccessfulTask()
            throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final Duration minDuration = Duration.ofMinutes(5);
            final GoogleFitSessionSyncOptions options =
                new GoogleFitSessionSyncOptions.Builder(fixedClock).setMinimumSessionDuration(minDuration)
                    .setDateRange(startDate, endDate)
                    .build();

            final Date startRequestDate =
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant());
            final Date endRequestDate =
                Date.from(LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant());

            final GFSessionBundle stubSessionBundle = mock(GFSessionBundle.class);

            when(mockedGFClientWrapper.getSessions(startRequestDate, endRequestDate, minDuration))
                .thenReturn(Tasks.forResult(Arrays.asList(stubSessionBundle)));
            when(mockedGFSyncMetadataStore.isNeededToSyncSessionBundle(stubSessionBundle)).thenReturn(true);
            final ApiCall mockedApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncSessions(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getSessions(startRequestDate, endRequestDate, minDuration);
            // should ask the sync metadata store about session
            verify(mockedGFSyncMetadataStore).isNeededToSyncSessionBundle(stubSessionBundle);
            // should save the metadata
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfSessions(Arrays.asList(stubSessionBundle));
            // should send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should try to send the sessions data",
                    Arrays.asList(stubSessionBundle),
                    uploadData.getSessionsData());
                return true;
            }));

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF sessions with date range [2020-09-30T22:00:00.000Z, 2020-10-02T21:59:59.999Z]",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending new GF data: GFUploadData{calories=0, steps=0, heartRates=0, sessions=1}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: succeeded to send GF data", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }
    }

    public static class SyncProfileTest extends GivenRobolectricContext {
        final String currentInstant = "2020-10-05T09:30:00Z";
        final TimeZone testTimeZone = TimeZone.getTimeZone("Europe/Zurich");
        final ZoneId testZoneId = testTimeZone.toZoneId();
        final Clock fixedClock = Clock.fixed(Instant.parse(currentInstant), testZoneId);
        final GFWeightDataPoint testWeightDataPoint =
            new GFWeightDataPoint(75.33f, Date.from(Instant.parse("2020-10-01T10:00:05Z")));
        final GFHeightDataPoint testHeightDataPoint =
            new GFHeightDataPoint(182.9f, Date.from(Instant.parse("2020-10-01T10:00:05Z")));

        GFDataManager subject;
        GFClientWrapper mockedGFClientWrapper;
        GFDataUtils gfDataUtilsSpy;
        GFSyncMetadataStore mockedGFSyncMetadataStore;
        ActivitySourcesService mockedActivitySourcesService;

        @Before
        public void beforeTests() {
            mockedGFClientWrapper = mock(GFClientWrapper.class);
            mockedGFSyncMetadataStore = mock(GFSyncMetadataStore.class);
            mockedActivitySourcesService = mock(ActivitySourcesService.class);
            GFDataUtils gfDataUtils = new GFDataUtils(testZoneId, fixedClock);
            gfDataUtilsSpy = spy(gfDataUtils);
            subject = new GFDataManager(mockedGFClientWrapper,
                gfDataUtilsSpy,
                mockedGFSyncMetadataStore,
                mockedActivitySourcesService,
                null);
        }

        @Test
        public void syncProfile_noWeightFromGoogleFit_returnsSuccessfulTaskWithFalse()
            throws ExecutionException, InterruptedException {
            final GoogleFitProfileSyncOptions options =
                new GoogleFitProfileSyncOptions.Builder().include(FitnessMetricsType.WEIGHT).build();
            when(mockedGFClientWrapper.getLastKnownWeight()).thenReturn(Tasks.forResult(null));

            Task<Boolean> result = subject.syncProfile(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            assertFalse("task result should be false", result.getResult());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getLastKnownWeight();
            // shouldn't request the last known height
            verify(mockedGFClientWrapper, never()).getLastKnownHeight();
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF profile metrics (WEIGHT)",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no the updated profile parameters to send",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncProfile_alreadySyncedWeightFromGoogleFit_returnsSuccessfulTaskWithFalse()
            throws ExecutionException, InterruptedException {
            final GoogleFitProfileSyncOptions options =
                new GoogleFitProfileSyncOptions.Builder().include(FitnessMetricsType.WEIGHT).build();
            when(mockedGFClientWrapper.getLastKnownWeight()).thenReturn(Tasks.forResult(testWeightDataPoint));
            when(mockedGFSyncMetadataStore.isNeededToSyncWeight(testWeightDataPoint)).thenReturn(false);

            Task<Boolean> result = subject.syncProfile(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            assertFalse("task result should be false", result.getResult());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getLastKnownWeight();
            // shouldn't request the last known height
            verify(mockedGFClientWrapper, never()).getLastKnownHeight();
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should ask the metadata store about the weight
            verify(mockedGFSyncMetadataStore).isNeededToSyncWeight(testWeightDataPoint);
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 2, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF profile metrics (WEIGHT)",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: no the updated profile parameters to send",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncProfile_notSyncedWeightWithFailedApiRequest_returnsFailedTask() throws InterruptedException {
            final GoogleFitProfileSyncOptions options =
                new GoogleFitProfileSyncOptions.Builder().include(FitnessMetricsType.WEIGHT).build();
            when(mockedGFClientWrapper.getLastKnownWeight()).thenReturn(Tasks.forResult(testWeightDataPoint));
            when(mockedGFSyncMetadataStore.isNeededToSyncWeight(testWeightDataPoint)).thenReturn(true);

            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            final ApiExceptions.BadRequestException apiCallException =
                new ApiExceptions.BadRequestException("Bad request");
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.error(apiCallException));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.updateProfileOnBehalfOfGoogleFit(any())).thenReturn(mockedApiCall);

            Task<Boolean> result = subject.syncProfile(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) { /* expected exception */ }

            assertFalse("should return failed task", result.isSuccessful());
            assertThat(result.getException(), instanceOf(CommonException.class));
            CommonException exception = (CommonException) result.getException();
            assertEquals("should have exception message", "Failed to send data to the server", exception.getMessage());
            assertEquals("should have exception cause", apiCallException, exception.getCause());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getLastKnownWeight();
            // shouldn't request the last known height
            verify(mockedGFClientWrapper, never()).getLastKnownHeight();
            // should ask the activity sources service
            verify(mockedActivitySourcesService).updateProfileOnBehalfOfGoogleFit(any());
            // should ask the metadata store about the weight
            verify(mockedGFSyncMetadataStore).isNeededToSyncWeight(testWeightDataPoint);
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF profile metrics (WEIGHT)",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending the updated profile parameters: GFSynchronizableProfileParams{weight=\"75.33\"}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: failed to send the profile data: Bad request",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncProfile_notSyncedWeightWithSuccessfulApiRequest_returnsFailedTask()
            throws InterruptedException {
            final GoogleFitProfileSyncOptions options =
                new GoogleFitProfileSyncOptions.Builder().include(FitnessMetricsType.WEIGHT).build();
            when(mockedGFClientWrapper.getLastKnownWeight()).thenReturn(Tasks.forResult(testWeightDataPoint));
            when(mockedGFSyncMetadataStore.isNeededToSyncWeight(testWeightDataPoint)).thenReturn(true);

            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.updateProfileOnBehalfOfGoogleFit(any())).thenReturn(mockedApiCall);

            Task<Boolean> result = subject.syncProfile(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) { /* expected exception */ }

            assertTrue("should return successful task", result.isSuccessful());
            assertTrue("task should have true value", result.getResult());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getLastKnownWeight();
            // shouldn't request the last known height
            verify(mockedGFClientWrapper, never()).getLastKnownHeight();
            // should ask the activity sources service
            verify(mockedActivitySourcesService).updateProfileOnBehalfOfGoogleFit(any());
            // should ask the metadata store about the weight
            verify(mockedGFSyncMetadataStore).isNeededToSyncWeight(testWeightDataPoint);
            // should ask the metadata store to save metadata of the weight
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfWeight(testWeightDataPoint);
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: start syncing GF profile metrics (WEIGHT)",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending the updated profile parameters: GFSynchronizableProfileParams{weight=\"75.33\"}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: succeeded to send the profile data", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }

        @Test
        public void syncProfile_notSyncedProfileFieldsWithSuccessfulApiRequest_returnsFailedTask()
            throws InterruptedException {
            final GoogleFitProfileSyncOptions options =
                new GoogleFitProfileSyncOptions.Builder().include(FitnessMetricsType.WEIGHT)
                    .include(FitnessMetricsType.HEIGHT)
                    .build();
            when(mockedGFClientWrapper.getLastKnownWeight()).thenReturn(Tasks.forResult(testWeightDataPoint));
            when(mockedGFClientWrapper.getLastKnownHeight()).thenReturn(Tasks.forResult(testHeightDataPoint));
            when(mockedGFSyncMetadataStore.isNeededToSyncWeight(testWeightDataPoint)).thenReturn(true);
            when(mockedGFSyncMetadataStore.isNeededToSyncHeight(testHeightDataPoint)).thenReturn(true);

            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                callback.onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.updateProfileOnBehalfOfGoogleFit(any())).thenReturn(mockedApiCall);

            Task<Boolean> result = subject.syncProfile(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) { /* expected exception */ }

            assertTrue("should return successful task", result.isSuccessful());
            assertTrue("task should have true value", result.getResult());
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getLastKnownWeight();
            // shouldn't request the last known height
            verify(mockedGFClientWrapper).getLastKnownHeight();
            // should ask the activity sources service
            verify(mockedActivitySourcesService).updateProfileOnBehalfOfGoogleFit(any());
            // should ask the metadata store about the weight and height
            verify(mockedGFSyncMetadataStore).isNeededToSyncWeight(testWeightDataPoint);
            verify(mockedGFSyncMetadataStore).isNeededToSyncHeight(testHeightDataPoint);
            // should ask the metadata store to save metadata of the weight and height
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfWeight(testWeightDataPoint);
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfHeight(testHeightDataPoint);
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);

            assertEquals("logger should have entries", 3, LOGGER.size());
            TimberLogEntry logEntry = LOGGER.removeFirst();
            assertThat(logEntry.getMessage(),
                containsString("[activitysources] GFDataManager: start syncing GF profile metrics"));
            assertThat(logEntry.getMessage(), containsString("HEIGHT"));
            assertThat(logEntry.getMessage(), containsString("WEIGHT"));
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals(
                "[activitysources] GFDataManager: sending the updated profile parameters: GFSynchronizableProfileParams{height=\"182.9\", weight=\"75.33\"}",
                logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
            logEntry = LOGGER.removeFirst();
            assertEquals("[activitysources] GFDataManager: succeeded to send the profile data", logEntry.getMessage());
            assertEquals(Log.DEBUG, logEntry.getPriority());
        }
    }
}
