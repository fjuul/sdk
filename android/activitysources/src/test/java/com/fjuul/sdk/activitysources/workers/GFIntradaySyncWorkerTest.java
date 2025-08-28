package com.fjuul.sdk.activitysources.workers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.ActivitySourceConnection;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManager;
import com.fjuul.sdk.activitysources.entities.ActivitySourcesManagerConfig;
import com.fjuul.sdk.activitysources.entities.FitnessMetricsType;
import com.fjuul.sdk.activitysources.entities.GoogleFitActivitySource;
import com.fjuul.sdk.activitysources.entities.GoogleFitIntradaySyncOptions;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;

import android.content.Context;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker.Result;
import androidx.work.testing.TestWorkerBuilder;

@RunWith(Enclosed.class)
public class GFIntradaySyncWorkerTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    private abstract static class GivenRobolectricContext {}

    public static class GetOrInitializeActivitySourcesManagerTests extends GivenRobolectricContext {
        GFIntradaySyncWorker subject;
        TestWorkerBuilder<GFIntradaySyncWorker> workerBuilder;

        @Before
        public void setUp() {
            final Context context = ApplicationProvider.getApplicationContext();
            final Executor executor = Executors.newSingleThreadExecutor();
            workerBuilder = TestWorkerBuilder.from(context, GFIntradaySyncWorker.class, executor);
        }

        @Test
        public void getOrInitializeActivitySourcesManager_whenNoInitializedInstance_initializesAndReturnsIt() {
            try (MockedStatic<ActivitySourcesManager> sourcesManagerStaticMock =
                mockStatic(ActivitySourcesManager.class)) {
                sourcesManagerStaticMock.when(ActivitySourcesManager::getInstance)
                    .thenThrow(IllegalStateException.class)
                    .thenReturn(null);

                Data inputData = new Data.Builder().putString("USER_TOKEN", "USER1")
                    .putString("USER_SECRET", "TOP_SECRET")
                    .putString("API_KEY", "FJUUL_API_KEY")
                    .putString("BASE_URL", "https://fjuul.com")
                    .build();
                subject = (GFIntradaySyncWorker) workerBuilder.setInputData(inputData).build();
                subject.getOrInitializeActivitySourcesManager();

                final ArgumentCaptor<ApiClient> apiClientCaptor = ArgumentCaptor.forClass(ApiClient.class);
                final ArgumentCaptor<ActivitySourcesManagerConfig> configCaptor =
                    ArgumentCaptor.forClass(ActivitySourcesManagerConfig.class);
                sourcesManagerStaticMock.verify(() -> {
                    ActivitySourcesManager.initialize(apiClientCaptor.capture(), configCaptor.capture(), true);
                });
                final ApiClient apiClient = apiClientCaptor.getValue();
                assertEquals("api client should be initialized with user token from the input data",
                    "USER1",
                    apiClient.getUserToken());
                assertEquals("api client should be initialized with user secret from the input data",
                    "TOP_SECRET",
                    apiClient.getUserSecret());
                assertEquals("api client should be initialized with api key from the input data",
                    "FJUUL_API_KEY",
                    apiClient.getApiKey());
                assertEquals("api client should be initialized with base url from the input data",
                    "https://fjuul.com",
                    apiClient.getBaseUrl());
                final ActivitySourcesManagerConfig config = configCaptor.getValue();
                assertEquals("config should have the untouched background mode",
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    config.getGoogleFitIntradayBackgroundSyncMode());
                assertEquals("config should have the untouched background mode",
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    config.getGoogleFitSessionsBackgroundSyncMode());
                assertEquals("config should have the empty set of collectable fitness metrics",
                    Collections.emptySet(),
                    config.getCollectableFitnessMetrics());
            }
        }
    }

    public static class DoWorkTests extends GivenRobolectricContext {
        // NOTE: we use the spy here to mock the initialization of ActivitySourcesManager
        GFIntradaySyncWorker spySubject;
        TestWorkerBuilder<GFIntradaySyncWorker> workerBuilder;
        static PausedExecutorService pausedExecutor = new PausedExecutorService();

        @Before
        public void setUp() {
            final Context context = ApplicationProvider.getApplicationContext();
            final Executor executor = Executors.newSingleThreadExecutor();
            workerBuilder = TestWorkerBuilder.from(context, GFIntradaySyncWorker.class, executor);
        }

        @AfterClass
        public static void tearDown() {
            pausedExecutor.shutdown();
        }

        @Test
        public void doWork_whenNoCurrentGFConnection_returnsSuccessfulResult()
            throws ExecutionException, InterruptedException {
            final ActivitySourcesManager mockedSourcesManager = mock(ActivitySourcesManager.class);
            when(mockedSourcesManager.getCurrent()).thenReturn(Collections.emptyList());

            spySubject = spy(workerBuilder.build());
            doReturn(mockedSourcesManager).when(spySubject).getOrInitializeActivitySourcesManager();

            final Future<Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final Result result = futureResult.get();

            assertThat("should return successful result", result, is(Result.success()));
            // should ask the sources manager for getting current connections
            verify(mockedSourcesManager).getCurrent();
            // no more interactions with the sources manager
            verifyNoMoreInteractions(mockedSourcesManager);
        }

        @Test
        public void doWork_whenTheSyncReturnsExceptionForFewFitnessMetrics_returnsFailedResult()
            throws ExecutionException, InterruptedException {
            final ActivitySourcesManager mockedSourcesManager = mock(ActivitySourcesManager.class);
            final ActivitySourceConnection mockedGfSourceConnection = mock(ActivitySourceConnection.class);
            final GoogleFitActivitySource mockedGoogleFit = mock(GoogleFitActivitySource.class);
            final CommonException gfException = new CommonException("Something went wrong");
            doAnswer((invocation -> {
                Callback<Void> callback = invocation.getArgument(1, Callback.class);
                callback.onResult(com.fjuul.sdk.core.entities.Result.error(gfException));
                return null;
            })).when(mockedGoogleFit).syncIntradayMetrics(any(), any());
            when(mockedGfSourceConnection.getActivitySource()).thenReturn(mockedGoogleFit);
            when(mockedSourcesManager.getCurrent()).thenReturn(Arrays.asList(mockedGfSourceConnection));

            final Data inputData = new Data.Builder()
                .putStringArray("INTRADAY_METRICS", new String[] {"INTRADAY_CALORIES", "INTRADAY_STEPS"})
                .build();
            spySubject = spy((GFIntradaySyncWorker) workerBuilder.setInputData(inputData).build());
            doReturn(mockedSourcesManager).when(spySubject).getOrInitializeActivitySourcesManager();

            final Future<Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final Result result = futureResult.get();

            assertThat("should return the failure", result, is(Result.failure()));
            // should ask the sources manager for getting current connections
            verify(mockedSourcesManager).getCurrent();
            // no more interactions with the sources manager
            verifyNoMoreInteractions(mockedSourcesManager);
            final ArgumentCaptor<GoogleFitIntradaySyncOptions> syncOptionsCaptor =
                ArgumentCaptor.forClass(GoogleFitIntradaySyncOptions.class);
            verify(mockedGoogleFit).syncIntradayMetrics(syncOptionsCaptor.capture(), any());
            final GoogleFitIntradaySyncOptions syncOptions = syncOptionsCaptor.getValue();
            assertEquals("sync options should have the right start time",
                LocalDate.now().minusDays(2),
                syncOptions.getStartDate());
            assertEquals("sync options should have the right end time", LocalDate.now(), syncOptions.getEndDate());
            assertEquals("should transform raw input data to fitness metrics",
                Stream.of(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES)
                    .collect(Collectors.toSet()),
                syncOptions.getMetrics());
        }

        @Test
        public void doWork_whenTheSyncReturnsSuccessfulResultForAllFitnessMetrics_returnsSuccessfulResult()
            throws ExecutionException, InterruptedException {
            final ActivitySourcesManager mockedSourcesManager = mock(ActivitySourcesManager.class);
            final ActivitySourceConnection mockedGfSourceConnection = mock(ActivitySourceConnection.class);
            final GoogleFitActivitySource mockedGoogleFit = mock(GoogleFitActivitySource.class);
            doAnswer((invocation -> {
                Callback<Void> callback = invocation.getArgument(1, Callback.class);
                callback.onResult(com.fjuul.sdk.core.entities.Result.value(null));
                return null;
            })).when(mockedGoogleFit).syncIntradayMetrics(any(), any());
            when(mockedGfSourceConnection.getActivitySource()).thenReturn(mockedGoogleFit);
            when(mockedSourcesManager.getCurrent()).thenReturn(Arrays.asList(mockedGfSourceConnection));

            final Data inputData =
                new Data.Builder()
                    .putStringArray("INTRADAY_METRICS",
                        new String[] {"INTRADAY_STEPS", "INTRADAY_CALORIES", "INTRADAY_HEART_RATE"})
                    .build();
            spySubject = spy((GFIntradaySyncWorker) workerBuilder.setInputData(inputData).build());
            doReturn(mockedSourcesManager).when(spySubject).getOrInitializeActivitySourcesManager();

            final Future<Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final Result result = futureResult.get();

            assertThat("should return the successful result", result, is(Result.success()));
            // should ask the sources manager for getting current connections
            verify(mockedSourcesManager).getCurrent();
            // no more interactions with the sources manager
            verifyNoMoreInteractions(mockedSourcesManager);
            final ArgumentCaptor<GoogleFitIntradaySyncOptions> syncOptionsCaptor =
                ArgumentCaptor.forClass(GoogleFitIntradaySyncOptions.class);
            verify(mockedGoogleFit).syncIntradayMetrics(syncOptionsCaptor.capture(), any());
            final GoogleFitIntradaySyncOptions syncOptions = syncOptionsCaptor.getValue();
            assertEquals("sync options should have the right start time",
                LocalDate.now().minusDays(2),
                syncOptions.getStartDate());
            assertEquals("sync options should have the right end time", LocalDate.now(), syncOptions.getEndDate());
            assertEquals("should transform raw input data to fitness metrics",
                Stream
                    .of(FitnessMetricsType.INTRADAY_STEPS,
                        FitnessMetricsType.INTRADAY_CALORIES,
                        FitnessMetricsType.INTRADAY_HEART_RATE)
                    .collect(Collectors.toSet()),
                syncOptions.getMetrics());
        }
    }
}
