package com.fjuul.sdk.activitysources.entities;

import android.os.Build;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.exceptions.ApiExceptions;
import com.fjuul.sdk.http.utils.ApiCall;
import com.fjuul.sdk.http.utils.ApiCallCallback;
import com.fjuul.sdk.http.utils.ApiCallResult;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GoogleFitDataManagerTest {
    public static final ExecutorService testExecutor = Executors.newSingleThreadExecutor();

    @AfterClass
    public static void shutdownExecutor() {
        testExecutor.shutdownNow();
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static class SyncIntradayMetricsTest extends GFClientWrapperTest.GivenRobolectricContext {
        final String currentInstant = "2020-10-05T09:30:00Z";
        final TimeZone testTimeZone = TimeZone.getTimeZone("Europe/Zurich");
        final ZoneId testZoneId = testTimeZone.toZoneId();
        final Clock fixedClock = Clock.fixed(Instant.parse(currentInstant), testZoneId);

        GoogleFitDataManager subject;
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
            subject = new GoogleFitDataManager(mockedGFClientWrapper, gfDataUtilsSpy, mockedGFSyncMetadataStore, mockedActivitySourcesService);
        }

        @Test
        public void syncIntradayMetrics_emptyGFDataResponseForCalories_returnsSuccessfulTask() throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder()
                .include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
                .setDateRange(startDate, endDate)
                .build();

            final Date startRequestDate = Date.from(
                LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()
            );
            final Date endRequestDate = Date.from(
                LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant()
            );

            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate)).
                thenReturn(Tasks.forResult(Collections.emptyList()));

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).adjustInputDatesForBatches(startDate, endDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                Collections.emptyList(),
                Duration.ofMinutes(30)
            );
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
            // should even not interact with the sync metadata store
            verifyNoInteractions(mockedGFSyncMetadataStore);
        }

        @Test
        public void syncIntradayMetrics_alreadySyncedGFDataResponseForCalories_returnsSuccessfulTask() throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder()
                .include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
                .setDateRange(startDate, endDate)
                .build();

            final Date startRequestDate = Date.from(
                LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()
            );
            final Date endRequestDate = Date.from(
                LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant()
            );

            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(10f,
                    Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                    "derived:com.google.calories.expended:Brand:tracker"
                )
            ).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate)).
                thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(false);

            Task<Void> result = subject.syncIntradayMetrics(options);
            testExecutor.submit(() -> Tasks.await(result)).get();

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).adjustInputDatesForBatches(startDate, endDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30)
            );
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(batch -> {
                return batch.getPoints().equals(calories) &&
                    batch.getStartTime().equals(Date.from(Instant.parse("2020-10-01T09:00:00Z"))) &&
                    batch.getEndTime().equals(Date.from(Instant.parse("2020-10-01T09:30:00Z")));
            }));
            // shouldn't interact with the activity sources service
            verifyNoInteractions(mockedActivitySourcesService);
        }

        @Test
        public void syncIntradayMetrics_notSyncedGFDataResponseForCaloriesWithFailedApiRequest_returnsFailedTask() throws InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder()
                .include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
                .setDateRange(startDate, endDate)
                .build();

            final Date startRequestDate = Date.from(
                LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()
            );
            final Date endRequestDate = Date.from(
                LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant()
            );

            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(10f,
                    Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                    "derived:com.google.calories.expended:Brand:tracker"
                )
            ).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate)).
                thenReturn(Tasks.forResult(calories));
            when(mockedGFSyncMetadataStore.isNeededToSyncCaloriesBatch(any())).thenReturn(true);


            final ApiCall<Void> mockedApiCall = mock(ApiCall.class);
            final ApiExceptions.BadRequestException apiCallException = new ApiExceptions.BadRequestException("Bad request");
            doAnswer(invocation -> {
                    final ApiCallCallback<Void> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.error(apiCallException));
                    return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedActivitySourcesService.uploadGoogleFitData(any())).thenReturn(mockedApiCall);

            Task<Void> result = subject.syncIntradayMetrics(options);
            try {
                testExecutor.submit(() -> Tasks.await(result)).get();
            } catch (ExecutionException exception) { /* expected exception */ }

            assertFalse("should return failed task", result.isSuccessful());
            assertThat(result.getException(), instanceOf(CommonException.class));
            CommonException exception = (CommonException) result.getException();
            assertEquals("should have exception message",
                "Failed to send data to the server",
                exception.getMessage());
            assertEquals("should have exception cause",
                apiCallException,
                exception.getCause());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).adjustInputDatesForBatches(startDate, endDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30)
            );
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(batch -> {
                return batch.getPoints().equals(calories) &&
                    batch.getStartTime().equals(Date.from(Instant.parse("2020-10-01T09:00:00Z"))) &&
                    batch.getEndTime().equals(Date.from(Instant.parse("2020-10-01T09:30:00Z")));
            }));
            // should not store the metadata
            verifyNoMoreInteractions(mockedGFSyncMetadataStore);
            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should send the calories data", uploadData.getCaloriesData(), calories);
                return true;
            }));
        }

        @Test
        public void syncIntradayMetrics_notSyncedGFDataResponseForCaloriesWithSuccessfulApiRequest_returnsSuccessfulTask() throws ExecutionException, InterruptedException {
            final LocalDate startDate = LocalDate.parse("2020-10-01");
            final LocalDate endDate = LocalDate.parse("2020-10-02");
            final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder()
                .include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
                .setDateRange(startDate, endDate)
                .build();

            final Date startRequestDate = Date.from(
                LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()
            );
            final Date endRequestDate = Date.from(
                LocalDateTime.parse("2020-10-02T23:59:59.999").atZone(testZoneId).toInstant()
            );

            List<GFCalorieDataPoint> calories = Stream.of(
                new GFCalorieDataPoint(10f,
                    Date.from(Instant.parse("2020-10-01T09:01:00Z")),
                    "derived:com.google.calories.expended:Brand:tracker"
                )
            ).collect(Collectors.toList());
            when(mockedGFClientWrapper.getCalories(startRequestDate, endRequestDate)).
                thenReturn(Tasks.forResult(calories));
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
            } catch (ExecutionException exception) { /* expected exception */ }

            assertTrue("should return successful task", result.isSuccessful());
            // should adjust input dates for the batching
            verify(gfDataUtilsSpy).adjustInputDatesForBatches(startDate, endDate, Duration.ofMinutes(30));
            // should split the gf response into 30-minutes batches taking into account the local timezone
            verify(gfDataUtilsSpy).groupPointsIntoBatchesByDuration(
                Date.from(LocalDateTime.parse("2020-10-01T00:00:00").atZone(testZoneId).toInstant()),
                Date.from(LocalDateTime.parse("2020-10-03T00:00:00").atZone(testZoneId).toInstant()),
                calories,
                Duration.ofMinutes(30)
            );
            // should ask client-wrapper for data for the specified time interval in the local timezone
            verify(mockedGFClientWrapper).getCalories(startRequestDate, endRequestDate);
            // should ask the metadata store about the calories batch
            verify(mockedGFSyncMetadataStore).isNeededToSyncCaloriesBatch(argThat(batch -> {
                return batch.getPoints().equals(calories) &&
                    batch.getStartTime().equals(Date.from(Instant.parse("2020-10-01T09:00:00Z"))) &&
                    batch.getEndTime().equals(Date.from(Instant.parse("2020-10-01T09:30:00Z")));
            }));
            // should ask the metadata store to save the metadata
            verify(mockedGFSyncMetadataStore).saveSyncMetadataOfCalories(argThat(batch -> {
                return batch.getPoints().equals(calories) &&
                    batch.getStartTime().equals(Date.from(Instant.parse("2020-10-01T09:00:00Z"))) &&
                    batch.getEndTime().equals(Date.from(Instant.parse("2020-10-01T09:30:00Z")));
            }));
            // should try to send the data to the server
            verify(mockedActivitySourcesService).uploadGoogleFitData(argThat(uploadData -> {
                assertEquals("should send the calories data", uploadData.getCaloriesData(), calories);
                return true;
            }));
        }
    }
}
