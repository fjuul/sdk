package com.fjuul.sdk.activitysources.workers;

import static androidx.work.ListenableWorker.Result.success;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
import com.fjuul.sdk.activitysources.entities.GoogleFitProfileSyncOptions;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;

import android.content.Context;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;

@RunWith(Enclosed.class)
public class GFProfileSyncWorkerTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    private abstract static class GivenRobolectricContext {}

    public static class GetOrInitializeActivitySourcesManagerTests extends GivenRobolectricContext {
        GFProfileSyncWorker subject;
        TestWorkerBuilder<GFProfileSyncWorker> workerBuilder;

        @Before
        public void setUp() {
            final Context context = ApplicationProvider.getApplicationContext();
            final Executor executor = Executors.newSingleThreadExecutor();
            workerBuilder = TestWorkerBuilder.from(context, GFProfileSyncWorker.class, executor);
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
                subject = (GFProfileSyncWorker) workerBuilder.setInputData(inputData).build();
                subject.getOrInitializeActivitySourcesManager();

                final ArgumentCaptor<ApiClient> apiClientCaptor = ArgumentCaptor.forClass(ApiClient.class);
                final ArgumentCaptor<ActivitySourcesManagerConfig> configCaptor =
                    ArgumentCaptor.forClass(ActivitySourcesManagerConfig.class);
                sourcesManagerStaticMock.verify(() -> {
                    ActivitySourcesManager.initialize(apiClientCaptor.capture(), configCaptor.capture());
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
                    config.getGoogleFitProfileBackgroundSyncMode());
                assertEquals("config should have the empty set of collectable fitness metrics",
                    Collections.emptySet(),
                    config.getCollectableFitnessMetrics());
            }
        }
    }

    public static class DoWorkTests extends GivenRobolectricContext {
        // NOTE: we use the spy here to mock the initialization of ActivitySourcesManager
        GFProfileSyncWorker spySubject;
        TestWorkerBuilder<GFProfileSyncWorker> workerBuilder;
        static PausedExecutorService pausedExecutor = new PausedExecutorService();

        @Before
        public void setUp() {
            final Context context = ApplicationProvider.getApplicationContext();
            final Executor executor = Executors.newSingleThreadExecutor();
            workerBuilder = TestWorkerBuilder.from(context, GFProfileSyncWorker.class, executor);
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

            final Future<ListenableWorker.Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final ListenableWorker.Result result = futureResult.get();

            assertThat("should return successful result", result, is(success()));
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
            final GoogleFitActivitySourceExceptions.CommonException gfException =
                new GoogleFitActivitySourceExceptions.CommonException("Something went wrong");
            doAnswer((invocation -> {
                Callback<Boolean> callback = invocation.getArgument(1, Callback.class);
                callback.onResult(com.fjuul.sdk.core.entities.Result.error(gfException));
                return null;
            })).when(mockedGoogleFit).syncProfile(any(), any());
            when(mockedGfSourceConnection.getActivitySource()).thenReturn(mockedGoogleFit);
            when(mockedSourcesManager.getCurrent()).thenReturn(Arrays.asList(mockedGfSourceConnection));

            final Data inputData =
                new Data.Builder().putStringArray("PROFILE_METRICS", new String[] {"HEIGHT", "WEIGHT"}).build();
            spySubject = spy((GFProfileSyncWorker) workerBuilder.setInputData(inputData).build());
            doReturn(mockedSourcesManager).when(spySubject).getOrInitializeActivitySourcesManager();

            final Future<ListenableWorker.Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final ListenableWorker.Result result = futureResult.get();

            assertThat("should return the failure", result, is(ListenableWorker.Result.failure()));
            // should ask the sources manager for getting current connections
            verify(mockedSourcesManager).getCurrent();
            // no more interactions with the sources manager
            verifyNoMoreInteractions(mockedSourcesManager);
            final ArgumentCaptor<GoogleFitProfileSyncOptions> syncOptionsCaptor =
                ArgumentCaptor.forClass(GoogleFitProfileSyncOptions.class);
            verify(mockedGoogleFit).syncProfile(syncOptionsCaptor.capture(), any());
            final GoogleFitProfileSyncOptions syncOptions = syncOptionsCaptor.getValue();
            assertEquals("should transform raw input data to fitness metrics",
                Stream.of(FitnessMetricsType.HEIGHT, FitnessMetricsType.WEIGHT).collect(Collectors.toSet()),
                syncOptions.getMetrics());
        }

        @Test
        public void doWork_whenTheSyncReturnsSuccessfulResultForAllFitnessMetrics_returnsSuccessfulResult()
            throws ExecutionException, InterruptedException {
            final ActivitySourcesManager mockedSourcesManager = mock(ActivitySourcesManager.class);
            final ActivitySourceConnection mockedGfSourceConnection = mock(ActivitySourceConnection.class);
            final GoogleFitActivitySource mockedGoogleFit = mock(GoogleFitActivitySource.class);
            doAnswer((invocation -> {
                Callback<Boolean> callback = invocation.getArgument(1, Callback.class);
                callback.onResult(com.fjuul.sdk.core.entities.Result.value(true));
                return null;
            })).when(mockedGoogleFit).syncProfile(any(), any());
            when(mockedGfSourceConnection.getActivitySource()).thenReturn(mockedGoogleFit);
            when(mockedSourcesManager.getCurrent()).thenReturn(Arrays.asList(mockedGfSourceConnection));

            final Data inputData =
                new Data.Builder().putStringArray("PROFILE_METRICS", new String[] {"HEIGHT", "WEIGHT"}).build();
            spySubject = spy((GFProfileSyncWorker) workerBuilder.setInputData(inputData).build());
            doReturn(mockedSourcesManager).when(spySubject).getOrInitializeActivitySourcesManager();

            final Future<ListenableWorker.Result> futureResult = pausedExecutor.submit(spySubject::doWork);
            pausedExecutor.runAll();
            final ListenableWorker.Result result = futureResult.get();

            assertThat("should return the successful result", result, is(success()));
            // should ask the sources manager for getting current connections
            verify(mockedSourcesManager).getCurrent();
            // no more interactions with the sources manager
            verifyNoMoreInteractions(mockedSourcesManager);
            final ArgumentCaptor<GoogleFitProfileSyncOptions> syncOptionsCaptor =
                ArgumentCaptor.forClass(GoogleFitProfileSyncOptions.class);
            verify(mockedGoogleFit).syncProfile(syncOptionsCaptor.capture(), any());
            final GoogleFitProfileSyncOptions syncOptions = syncOptionsCaptor.getValue();
            assertEquals("should transform raw input data to fitness metrics",
                Stream.of(FitnessMetricsType.HEIGHT, FitnessMetricsType.WEIGHT).collect(Collectors.toSet()),
                syncOptions.getMetrics());
        }
    }
}
