package com.fjuul.sdk.activitysources.entities;

import static android.os.Looper.getMainLooper;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceResolver;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceWorkScheduler;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourcesStateStore;
import com.fjuul.sdk.activitysources.entities.internal.BackgroundWorkManager;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.activitysources.workers.GFSyncWorker;
import com.fjuul.sdk.activitysources.workers.HCIntradaySyncWorker;
import com.fjuul.sdk.activitysources.workers.HCProfileSyncWorker;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.IStorage;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.test.LoggableTestSuite;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.SettableFuture;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Operation;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestListenableWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;
import okhttp3.OkHttpClient;

@RunWith(Enclosed.class)
public class ActivitySourcesManagerTest {

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext extends LoggableTestSuite {}

    private static void awaitAsyncManagerWork() {
        ActivitySourcesManager.awaitWorkManagerExecutorForTesting();
        shadowOf(getMainLooper()).idle();
    }

    @RunWith(Enclosed.class)
    public static class InstanceMethods {
        public static class ConnectTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedActivitySourcesService;
            ActivitySourcesStateStore mockedStateStore;
            CopyOnWriteArrayList<TrackerConnection> trackerConnections;
            Context mockContext;

            @Before
            public void beforeTest() {
                mockContext = mock(Context.class);
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                final ActivitySourceResolver activitySourceResolver = new ActivitySourceResolver();
                trackerConnections = new CopyOnWriteArrayList<>();
                subject = new ActivitySourcesManager(mockedConfig,
                    mockedBackgroundWorkManager,
                    mockedActivitySourcesService,
                    mockedStateStore,
                    activitySourceResolver,
                    trackerConnections,
                    mockContext);
            }

            @Test
            public void connect_whenGoogleFit_bringsConnectingIntentToCallback() {
                final GoogleFitActivitySource googleFit = mock(GoogleFitActivitySource.class);
                final Callback<Intent> mockedCallback = mock(Callback.class);
                final Intent testIntent = new Intent();
                when(googleFit.buildIntentRequestingFitnessPermissions()).thenReturn(testIntent);

                subject.connect(googleFit, mockedCallback);

                // should ask google fit activity source for building the connecting intent
                verify(googleFit).buildIntentRequestingFitnessPermissions();
                verifyNoInteractions(mockedActivitySourcesService);
                final ArgumentCaptor<Result<Intent>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<Intent> callbackResult = callbackResultCaptor.getValue();
                assertFalse("callback should have successful result", callbackResult.isError());
                assertEquals("should return the intent requesting permissions from the GF activity source",
                    testIntent,
                    callbackResult.getValue());
            }

            @Test
            public void connect_whenExternalActivitySourceWithSuccessfulApiRequest_bringsConnectingIntentToCallback() {
                final GarminActivitySource garmin = GarminActivitySource.getInstance();
                final Callback<Intent> mockedCallback = mock(Callback.class);

                final ExternalAuthenticationFlowRequired mockedConnectionResult =
                    mock(ExternalAuthenticationFlowRequired.class);
                final String externalConnectionUrl = "https://garmin.com/follow-me";
                when(mockedConnectionResult.getUrl()).thenReturn(externalConnectionUrl);
                final ApiCall<ConnectionResult> mockedApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(mockedConnectionResult));
                    return null;
                }).when(mockedApiCall).enqueue(any());
                when(mockedActivitySourcesService.connect("garmin")).thenReturn(mockedApiCall);

                subject.connect(garmin, mockedCallback);
                // should ask the activity sources service to try to connect
                verify(mockedActivitySourcesService).connect("garmin");

                final ArgumentCaptor<Result<Intent>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<Intent> callbackResult = callbackResultCaptor.getValue();
                assertFalse("callback should have successful result", callbackResult.isError());
                final Intent connectingIntent = callbackResult.getValue();
                assertEquals("result should have the connecting intent",
                    Intent.ACTION_VIEW,
                    connectingIntent.getAction());
                assertEquals("result should have the connecting intent",
                    externalConnectionUrl,
                    connectingIntent.getData().toString());
            }

            @Test
            public void connect_whenExternalActivitySourceWithFailedApiRequest_bringsErrorResultToCallback() {
                final GarminActivitySource garmin = GarminActivitySource.getInstance();
                final Callback<Intent> mockedCallback = mock(Callback.class);

                final ApiExceptions.BadRequestException apiCallException =
                    new ApiExceptions.BadRequestException("Bad request");
                final ApiCall<ConnectionResult> mockedApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.error(apiCallException));
                    return null;
                }).when(mockedApiCall).enqueue(any());
                when(mockedActivitySourcesService.connect("garmin")).thenReturn(mockedApiCall);

                subject.connect(garmin, mockedCallback);
                // should ask the activity sources service to try to connect
                verify(mockedActivitySourcesService).connect("garmin");

                final ArgumentCaptor<Result<Intent>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<Intent> callbackResult = callbackResultCaptor.getValue();
                assertTrue("callback should have unsuccessful result", callbackResult.isError());
                assertEquals("error should be the api error", apiCallException, callbackResult.getError());
            }
        }

        public static class DisconnectTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedSourcesService;
            ActivitySourcesStateStore mockedStateStore;
            ActivitySourceResolver activitySourceResolver;
            GoogleFitActivitySource mockedGoogleFit;
            HealthConnectActivitySource mockedHealthConnect;
            Context mockContext;

            @Before
            public void beforeTest() {
                mockContext = mock(Context.class);
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                mockedGoogleFit = mock(GoogleFitActivitySource.class);
                mockedHealthConnect = mock(HealthConnectActivitySource.class);
                activitySourceResolver = mock(ActivitySourceResolver.class);
                when(activitySourceResolver.getInstanceByTrackerValue("googlefit")).thenReturn(mockedGoogleFit);
                when(activitySourceResolver.getInstanceByTrackerValue("healthconnect")).thenReturn(mockedHealthConnect);
            }

            @Test
            @LooperMode(LooperMode.Mode.PAUSED)
            public void disconnect_whenGoogleFit_disconnectsAndRemoveConnectionFromCurrent() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    final GoogleFitActivitySource googleFit = mock(GoogleFitActivitySource.class);
                    final TrackerConnection gfTrackerConnection = new TrackerConnection("gf_c_id",
                        TrackerValue.GOOGLE_FIT.getValue(),
                        Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                        null);
                    final ActivitySourceConnection gfConnection =
                        new ActivitySourceConnection(gfTrackerConnection, googleFit);
                    final CopyOnWriteArrayList<TrackerConnection> trackerConnections =
                        Stream.of(gfTrackerConnection).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        activitySourceResolver,
                        trackerConnections,
                        mockContext);
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    when(googleFit.disable()).thenReturn(Tasks.forResult(null));
                    final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<ConnectionResult> callback =
                            invocation.getArgument(0, ApiCallCallback.class);
                        callback.onResult(null, ApiCallResult.value(null));
                        return null;
                    }).when(mockedDisconnectApiCall).enqueue(any());
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                    when(mockedSourcesService.disconnect(gfConnection)).thenReturn(mockedDisconnectApiCall);

                    subject.disconnect(gfConnection, mockedCallback);
                    // NOTE: execute all tasks posted to the main looper
                    shadowOf(getMainLooper()).idle();
                    awaitAsyncManagerWork();

                    // should revoke GoogleFit OAuth permissions
                    verify(googleFit).disable();
                    // should ask the sources service to disconnect
                    verify(mockedSourcesService).disconnect(gfConnection);
                    verify(mockedDisconnectApiCall).enqueue(any());
                    assertTrue("should remove the source connection from the current ones",
                        subject.getCurrent().isEmpty());
                    // should pass the changed connections to the activity sources state store
                    verify(mockedStateStore).setConnections(Collections.emptyList());
                    // should disable any background workers of GoogleFit
                    verify(mockedBackgroundWorkManager).cancelGFIntradaySyncWork();
                    verify(mockedBackgroundWorkManager).cancelGFSessionsSyncWork();
                    verify(mockedBackgroundWorkManager).cancelProfileSyncWork();
                    assertEquals("should set new connections in the subject",
                        Collections.emptyList(),
                        subject.getCurrent());
                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                }
            }

            @Test
            public void disconnect_whenExternalActivitySource_disconnectsAndRemoveConnectionFromCurrent() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    final FitbitActivitySource fitbit = FitbitActivitySource.getInstance();
                    final TrackerConnection fitbitTrackerConnection = new TrackerConnection("fitbit_c_id",
                        TrackerValue.FITBIT.getValue(),
                        Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                        null);
                    final ActivitySourceConnection fitbitConnection =
                        new ActivitySourceConnection(fitbitTrackerConnection, fitbit);
                    final CopyOnWriteArrayList<TrackerConnection> trackerConnections =
                        Stream.of(fitbitTrackerConnection).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        activitySourceResolver,
                        trackerConnections,
                        mockContext);
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<ConnectionResult> callback =
                            invocation.getArgument(0, ApiCallCallback.class);
                        callback.onResult(null, ApiCallResult.value(null));
                        return null;
                    }).when(mockedDisconnectApiCall).enqueue(any());
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                    when(mockedSourcesService.disconnect(fitbitConnection)).thenReturn(mockedDisconnectApiCall);

                    subject.disconnect(fitbitConnection, mockedCallback);
                    awaitAsyncManagerWork();

                    // should ask the sources service to disconnect
                    verify(mockedSourcesService).disconnect(fitbitConnection);
                    verify(mockedDisconnectApiCall).enqueue(any());
                    assertTrue("should remove the source connection from the current ones",
                        subject.getCurrent().isEmpty());
                    // should pass the changed connections to the activity sources state store
                    verify(mockedStateStore).setConnections(Collections.emptyList());
                    assertEquals("should set new connections in the subject",
                        Collections.emptyList(),
                        subject.getCurrent());
                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                }
            }

            @Test
            public void disconnect_whenWorkConfigurationFails_stillPersistsTheAcknowledgedDisconnect() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    final TrackerConnection trackerConnection = new TrackerConnection("fitbit_c_id",
                        TrackerValue.FITBIT.getValue(),
                        Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                        null);
                    final ActivitySourceConnection connection =
                        new ActivitySourceConnection(trackerConnection, FitbitActivitySource.getInstance());
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        activitySourceResolver,
                        new CopyOnWriteArrayList<>(Collections.singletonList(trackerConnection)),
                        mockContext);
                    final IllegalStateException schedulingFailure =
                        new IllegalStateException("WorkManager unavailable");
                    final ApiCall<Void> disconnectCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        invocation.getArgument(0, ApiCallCallback.class).onResult(null, ApiCallResult.value(null));
                        return null;
                    }).when(disconnectCall).enqueue(any());
                    when(mockedSourcesService.disconnect(connection)).thenReturn(disconnectCall);
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                    doThrow(schedulingFailure).when(mockedBackgroundWorkManager).cancelGFIntradaySyncWork();
                    final Callback<Void> callback = mock(Callback.class);

                    subject.disconnect(connection, callback);
                    awaitAsyncManagerWork();

                    assertTrue(subject.getCurrent().isEmpty());
                    verify(mockedStateStore).setConnections(Collections.emptyList());
                    final ArgumentCaptor<Result<Void>> resultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(callback).onResult(resultCaptor.capture());
                    assertTrue(resultCaptor.getValue().isError());
                    assertSame(schedulingFailure, resultCaptor.getValue().getError());
                }
            }

            @Test
            public void disconnect_whenAvailabilityLookupFails_stillRemovesAndPersistsTheConnection() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    final TrackerConnection trackerConnection = new TrackerConnection("fitbit_c_id",
                        TrackerValue.FITBIT.getValue(),
                        Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                        null);
                    final ActivitySourceConnection connection =
                        new ActivitySourceConnection(trackerConnection, FitbitActivitySource.getInstance());
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        activitySourceResolver,
                        new CopyOnWriteArrayList<>(Collections.singletonList(trackerConnection)),
                        mockContext);
                    final IllegalStateException availabilityFailure =
                        new IllegalStateException("Health Connect availability unavailable");
                    final ApiCall<Void> disconnectCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        invocation.getArgument(0, ApiCallCallback.class).onResult(null, ApiCallResult.value(null));
                        return null;
                    }).when(disconnectCall).enqueue(any());
                    when(mockedSourcesService.disconnect(connection)).thenReturn(disconnectCall);
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenThrow(availabilityFailure);
                    final Callback<Void> callback = mock(Callback.class);

                    subject.disconnect(connection, callback);
                    awaitAsyncManagerWork();

                    assertTrue(subject.getCurrent().isEmpty());
                    verify(mockedStateStore).setConnections(Collections.emptyList());
                    final ArgumentCaptor<Result<Void>> resultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(callback).onResult(resultCaptor.capture());
                    assertTrue(resultCaptor.getValue().isError());
                    assertSame(availabilityFailure, resultCaptor.getValue().getError());
                }
            }
        }

        public static class GetCurrentTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedSourcesService;
            ActivitySourcesStateStore mockedStateStore;
            ActivitySourceResolver activitySourceResolver;
            Context mockContext;

            @Before
            public void beforeTest() {
                mockContext = mock(Context.class);
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                activitySourceResolver = new ActivitySourceResolver();
            }

            @Test
            public void getCurrent_whenNoCurrentConnections_returnsEmptyList() {
                subject = new ActivitySourcesManager(mockedConfig,
                    mockedBackgroundWorkManager,
                    mockedSourcesService,
                    mockedStateStore,
                    activitySourceResolver,
                    new CopyOnWriteArrayList<>(),
                    mockContext);
                assertEquals(Collections.emptyList(), subject.getCurrent());
            }

            @Test
            public void getCurrent_withExistedTrackerConnections_returnsActivitySourceConnections() {
                final TrackerConnection fitbitTrackerConnection = new TrackerConnection("fitbit_c_id",
                    TrackerValue.FITBIT.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);
                final TrackerConnection healthkitTrackerConnection = new TrackerConnection("healthkit_c_id",
                    "healthkit",
                    Date.from(Instant.parse("2020-09-10T10:20:00Z")),
                    null);
                final CopyOnWriteArrayList<TrackerConnection> trackerConnections =
                    Stream.of(fitbitTrackerConnection, healthkitTrackerConnection)
                        .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                subject = new ActivitySourcesManager(mockedConfig,
                    mockedBackgroundWorkManager,
                    mockedSourcesService,
                    mockedStateStore,
                    activitySourceResolver,
                    trackerConnections,
                    mockContext);
                final List<ActivitySourceConnection> activitySourceConnections = subject.getCurrent();
                assertEquals("should have 2 activity source connections", 2, activitySourceConnections.size());
                final ActivitySourceConnection fitbitActivitySourceConnection = activitySourceConnections.get(0);
                assertEquals("the first activity source connection should have fitbit activity source",
                    FitbitActivitySource.getInstance(),
                    fitbitActivitySourceConnection.getActivitySource());
                assertEquals("the first activity source connection should have fitbit tracker",
                    fitbitTrackerConnection.getId(),
                    fitbitActivitySourceConnection.getId());
                final ActivitySourceConnection healthkitActivitySourceConnection = activitySourceConnections.get(1);
                assertThat("the second connection should have unknown activity source",
                    healthkitActivitySourceConnection.getActivitySource(),
                    instanceOf(UnknownActivitySource.class));
                assertEquals("the second connection should be healthkit tracker",
                    healthkitTrackerConnection.getId(),
                    healthkitActivitySourceConnection.getId());
            }
        }

        public static class RefreshCurrentTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedSourcesService;
            ActivitySourcesStateStore mockedStateStore;
            ActivitySourceResolver mockedActivitySourceResolver;
            Context mockContext;

            @Before
            public void beforeTest() {
                mockContext = mock(Context.class);
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                mockedActivitySourceResolver = mock(ActivitySourceResolver.class);
            }

            @Test
            public void refreshCurrent_whenGetNewConnectionsWithGoogleFitAndCallbackIsNull_refreshesCurrentConnections() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        mockedActivitySourceResolver,
                        new CopyOnWriteArrayList<>(),
                        mockContext);

                    final Date connectionCreatedAt = Date.from(Instant.parse("2020-09-10T10:05:00Z"));
                    final TrackerConnection gfTrackerConnection =
                        new TrackerConnection("gf_c_id", TrackerValue.GOOGLE_FIT.getValue(), connectionCreatedAt, null);

                    final TrackerConnection[] newConnections = new TrackerConnection[] {gfTrackerConnection};
                    final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<TrackerConnection[]> callback =
                            invocation.getArgument(0, ApiCallCallback.class);
                        callback.onResult(null, ApiCallResult.value(newConnections));
                        return null;
                    }).when(mockedGetConnectionsApiCall).enqueue(any());
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                    when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
                    GoogleFitActivitySource googleFitStub = mock(GoogleFitActivitySource.class);
                    final HealthConnectActivitySource healthConnect = mock(HealthConnectActivitySource.class);
                    when(mockedActivitySourceResolver.getInstanceByTrackerValue("googlefit")).thenReturn(googleFitStub);
                    when(mockedActivitySourceResolver.getInstanceByTrackerValue("healthconnect"))
                        .thenReturn(healthConnect);

                    subject.refreshCurrent(null);
                    awaitAsyncManagerWork();

                    // should ask the sources service to get fresh ones
                    verify(mockedSourcesService).getCurrentConnections();
                    verify(mockedGetConnectionsApiCall).enqueue(any());
                    // should pass new connections to the activity sources state store
                    verify(mockedStateStore).setConnections(Arrays.asList(newConnections));
                    List<ActivitySourceConnection> currentActivitySourceConnections = subject.getCurrent();
                    assertEquals("current connections should have 1 entry", 1, currentActivitySourceConnections.size());
                    ActivitySourceConnection gfActivitySourceConnection = currentActivitySourceConnections.get(0);
                    assertEquals("current connection should be the gf connection",
                        googleFitStub,
                        gfActivitySourceConnection.getActivitySource());
                    assertEquals("current connection should be the gf connection",
                        gfTrackerConnection.getId(),
                        gfActivitySourceConnection.getId());
                    assertEquals("current connection should be the gf connection",
                        gfTrackerConnection.getTracker(),
                        gfActivitySourceConnection.getTracker());
                    // should configure background works because of the presence of the google-fit tracker
                    verify(mockedBackgroundWorkManager).configureGFSyncWorks();
                    verify(mockedBackgroundWorkManager).configureProfileSyncWork();
                    // should set the lower date bound from the connection
                    verify(googleFitStub).setLowerDateBoundary(connectionCreatedAt);
                }
            }

            @Test
            public void refreshCurrent_whenGetNewConnectionsWithPolarAndCallbackIsNotNull_refreshesCurrentConnections() {
                try (MockedStatic<HealthConnectActivitySource> mocked = mockStatic(HealthConnectActivitySource.class)) {
                    subject = new ActivitySourcesManager(mockedConfig,
                        mockedBackgroundWorkManager,
                        mockedSourcesService,
                        mockedStateStore,
                        mockedActivitySourceResolver,
                        new CopyOnWriteArrayList<>(),
                        mockContext);

                    final TrackerConnection polarTrackerConnection = new TrackerConnection("polar_c_id",
                        TrackerValue.POLAR.getValue(),
                        Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                        null);

                    final TrackerConnection[] newConnections = new TrackerConnection[] {polarTrackerConnection};
                    final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<TrackerConnection[]> callback =
                            invocation.getArgument(0, ApiCallCallback.class);
                        callback.onResult(null, ApiCallResult.value(newConnections));
                        return null;
                    }).when(mockedGetConnectionsApiCall).enqueue(any());
                    mocked.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                        .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                    when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
                    final PolarActivitySource polarStub = mock(PolarActivitySource.class);
                    final GoogleFitActivitySource googleFitStub = mock(GoogleFitActivitySource.class);
                    final HealthConnectActivitySource healthConnect = mock(HealthConnectActivitySource.class);
                    when(mockedActivitySourceResolver.getInstanceByTrackerValue("polar")).thenReturn(polarStub);
                    when(mockedActivitySourceResolver.getInstanceByTrackerValue("googlefit")).thenReturn(googleFitStub);
                    when(mockedActivitySourceResolver.getInstanceByTrackerValue("healthconnect"))
                        .thenReturn(healthConnect);
                    final Callback<List<ActivitySourceConnection>> mockedCallback = mock(Callback.class);

                    subject.refreshCurrent(mockedCallback);
                    awaitAsyncManagerWork();

                    // should ask the sources service to get fresh ones
                    verify(mockedSourcesService).getCurrentConnections();
                    verify(mockedGetConnectionsApiCall).enqueue(any());
                    // should pass new connections to the activity sources state store
                    verify(mockedStateStore).setConnections(Arrays.asList(newConnections));
                    final List<ActivitySourceConnection> currentActivitySourceConnections = subject.getCurrent();
                    assertEquals("current connections should have 1 entry", 1, currentActivitySourceConnections.size());
                    final ActivitySourceConnection polarActivitySourceConnection =
                        currentActivitySourceConnections.get(0);
                    assertEquals("current connection should be polar",
                        polarStub,
                        polarActivitySourceConnection.getActivitySource());
                    assertEquals("current connection should be polar",
                        polarTrackerConnection.getId(),
                        polarActivitySourceConnection.getId());
                    assertEquals("current connection should be polar",
                        polarTrackerConnection.getTracker(),
                        polarActivitySourceConnection.getTracker());
                    // should cancel background works because of the absence of the google-fit tracker
                    verify(mockedBackgroundWorkManager).cancelGFIntradaySyncWork();
                    verify(mockedBackgroundWorkManager).cancelGFSessionsSyncWork();
                    verify(mockedBackgroundWorkManager).cancelProfileSyncWork();
                    // should disable the lower date bound
                    verify(googleFitStub).setLowerDateBoundary(null);
                    final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultArgumentCaptor =
                        ArgumentCaptor.forClass(Result.class);
                    // should pass new connections to the callback
                    verify(mockedCallback).onResult(callbackResultArgumentCaptor.capture());
                    final Result<List<ActivitySourceConnection>> callbackResult =
                        callbackResultArgumentCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                    assertEquals("callback result should have polar",
                        polarTrackerConnection.getId(),
                        callbackResult.getValue().get(0).getId());
                }
            }

            @Test
            public void refreshCurrent_whenApiCallFailsAndCallbackIsNotNull_bringsApiCallExceptionToCallback() {
                subject = new ActivitySourcesManager(mockedConfig,
                    mockedBackgroundWorkManager,
                    mockedSourcesService,
                    mockedStateStore,
                    mockedActivitySourceResolver,
                    new CopyOnWriteArrayList<>(),
                    mockContext);

                final ApiExceptions.BadRequestException apiCallException =
                    new ApiExceptions.BadRequestException("Bad request");
                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback =
                        invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.error(apiCallException));
                    return null;
                }).when(mockedGetConnectionsApiCall).enqueue(any());
                when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
                final Callback<List<ActivitySourceConnection>> mockedCallback = mock(Callback.class);

                subject.refreshCurrent(mockedCallback);

                // should ask the sources service to get fresh ones
                verify(mockedSourcesService).getCurrentConnections();
                verify(mockedGetConnectionsApiCall).enqueue(any());
                // should not interact with the state store
                verifyNoInteractions(mockedStateStore);
                // should not interact with background work manager
                verifyNoInteractions(mockedBackgroundWorkManager);
                final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultArgumentCaptor =
                    ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultArgumentCaptor.capture());
                final Result<List<ActivitySourceConnection>> callbackResult = callbackResultArgumentCaptor.getValue();
                assertTrue("callback should have unsuccessful result", callbackResult.isError());
                assertEquals("callback result should have the api call exception",
                    apiCallException,
                    callbackResult.getError());
            }
        }
    }

    public static class RefreshCurrentIfUploadRejectedTests extends GivenRobolectricContext {
        ActivitySourcesService mockedSourcesService;
        ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall;
        AtomicReference<ApiCallCallback<TrackerConnection[]>> pendingRefreshCallback;

        @Before
        public void beforeTest() {
            mockedSourcesService = mock(ActivitySourcesService.class);
            mockedGetConnectionsApiCall = mock(ApiCall.class);
            pendingRefreshCallback = new AtomicReference<>();
            doAnswer(invocation -> {
                pendingRefreshCallback.set(invocation.getArgument(0, ApiCallCallback.class));
                return null;
            }).when(mockedGetConnectionsApiCall).enqueue(any());
            when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
            ActivitySourcesManager.setInstance(new ActivitySourcesManager(mock(ActivitySourcesManagerConfig.class),
                mock(BackgroundWorkManager.class),
                mockedSourcesService,
                mock(ActivitySourcesStateStore.class),
                mock(ActivitySourceResolver.class),
                new CopyOnWriteArrayList<>(),
                mock(Context.class)));
        }

        @After
        public void afterTest() {
            ActivitySourcesManager.setInstance(null);
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenConflictOnBackgroundThread_blocksUntilRefreshCompletes()
            throws InterruptedException {
            final Exception uploadError =
                new ApiExceptions.CommonException("Failed to send data", new ApiExceptions.ConflictException("409"));
            final Thread syncThread =
                new Thread(() -> ActivitySourcesManager.refreshCurrentIfUploadRejected(uploadError));

            syncThread.start();

            verify(mockedGetConnectionsApiCall, timeout(1000)).enqueue(any());
            syncThread.join(200);
            assertTrue("should wait for the refresh to complete", syncThread.isAlive());

            pendingRefreshCallback.get()
                .onResult(mockedGetConnectionsApiCall, ApiCallResult.error(new ApiExceptions.BadRequestException("")));
            syncThread.join(1000);
            assertFalse("should return once the refresh completed", syncThread.isAlive());
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenNotConflict_doesNotRefresh() {
            ActivitySourcesManager.refreshCurrentIfUploadRejected(new ApiExceptions.BadRequestException("400"));

            verifyNoInteractions(mockedSourcesService);
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenConflictOnMainThread_refreshesWithoutBlocking() {
            final long startedAt = System.nanoTime();

            ActivitySourcesManager.refreshCurrentIfUploadRejected(new ApiExceptions.ConflictException("409"));

            verify(mockedGetConnectionsApiCall).enqueue(any());
            assertTrue("should not wait on the main thread",
                TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt) < 5);
        }
    }

    public static class AsyncWorkManagerTests extends GivenRobolectricContext {
        @Before
        public void beforeTest() {
            ActivitySourcesManager.setInstance(null);
            WorkManagerTestInitHelper.initializeTestWorkManager(ApplicationProvider.getApplicationContext(),
                new Configuration.Builder().setExecutor(new SynchronousExecutor()).build());
        }

        @After
        public void afterTest() {
            ActivitySourcesManager.setInstance(null);
        }

        @Test
        public void initializeAsync_whenWorkSetupSucceeds_publishesBeforeMainThreadCallback() {
            final Context context = ApplicationProvider.getApplicationContext();
            final ApiClient client = new ApiClient.Builder(context, "https://async.fjuul.test", "api-key")
                .setUserCredentials(new UserCredentials("async-user", "async-secret"))
                .build();
            final AtomicReference<Result<Void>> initializationResult = new AtomicReference<>();
            final AtomicBoolean callbackOnMainThread = new AtomicBoolean(false);

            ActivitySourcesManager.initializeAsync(client, ActivitySourcesManagerConfig.buildDefault(), result -> {
                initializationResult.set(result);
                callbackOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            });

            ActivitySourcesManager.awaitWorkManagerExecutorForTesting();
            shadowOf(getMainLooper()).idle();

            assertFalse(initializationResult.get().isError());
            assertTrue(callbackOnMainThread.get());
            assertSame(ActivitySourcesManager.getInstance(),
                ActivitySourcesManager.getOrInitializeForWorker(context,
                    new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, "async-user")
                        .putString(GFSyncWorker.KEY_USER_SECRET_ARG, "async-secret")
                        .putString(GFSyncWorker.KEY_API_KEY_ARG, "api-key")
                        .putString(GFSyncWorker.KEY_BASE_URL_ARG, "https://async.fjuul.test")
                        .build()));
        }

        @Test
        public void cancellationAttemptsEveryWorkNameAfterOneOperationFails() {
            final WorkManager workManager = mock(WorkManager.class);
            final Operation failedOperation = operationWithFailure(new IllegalStateException("cancel failed"));
            final Operation successfulOperation = operationWithSuccess();
            final AtomicInteger cancellationCount = new AtomicInteger();
            when(workManager.cancelUniqueWork(anyString())).thenAnswer(
                invocation -> cancellationCount.getAndIncrement() == 0 ? failedOperation : successfulOperation);

            assertThrows(IllegalStateException.class,
                () -> ActivitySourceWorkScheduler.cancelWorks(workManager,
                    ApplicationProvider.getApplicationContext()));

            verify(workManager, times(6)).cancelUniqueWork(anyString());
        }

        @Test
        public void failedCancellationRetainsItsRollbackSnapshot() {
            final Context context = ApplicationProvider.getApplicationContext();
            final WorkManager workManager = WorkManager.getInstance(context);
            final ActivitySourceWorkScheduler scheduler = new ActivitySourceWorkScheduler(workManager,
                "snapshot-user",
                "snapshot-secret",
                "snapshot-api-key",
                "https://snapshot.fjuul.test",
                context);
            scheduler.scheduleGFIntradaySyncWork(Collections.singleton(FitnessMetricsType.INTRADAY_HEART_RATE));

            final WorkManager failingWorkManager = spy(workManager);
            doAnswer(invocation -> {
                final String workName = invocation.getArgument(0);
                if (ActivitySourceWorkScheduler.GF_INTRADAY_SYNC_WORK_NAME.equals(workName)) {
                    return operationWithFailure(new IllegalStateException("cancel failed"));
                }
                return invocation.callRealMethod();
            }).when(failingWorkManager).cancelUniqueWork(anyString());

            assertThrows(IllegalStateException.class,
                () -> ActivitySourceWorkScheduler.cancelWorks(failingWorkManager, context));

            verify(failingWorkManager, times(6)).cancelUniqueWork(anyString());
            assertFalse(ActivitySourceWorkScheduler.snapshotWorks(workManager, context).isEmpty());
        }

        @Test
        public void snapshotWorks_recoversAtomicBackupWhenBaseFileIsMissing() throws Exception {
            final Context context = ApplicationProvider.getApplicationContext();
            final WorkManager workManager = WorkManager.getInstance(context);
            final ActivitySourceWorkScheduler scheduler = new ActivitySourceWorkScheduler(workManager,
                "backup-user",
                "backup-secret",
                "backup-api-key",
                "https://backup.fjuul.test",
                context);
            scheduler.scheduleGFIntradaySyncWork(Collections.singleton(FitnessMetricsType.INTRADAY_HEART_RATE));
            final String snapshotId =
                snapshotIdForWork(workManager, ActivitySourceWorkScheduler.GF_INTRADAY_SYNC_WORK_NAME);
            final File snapshot =
                snapshotFile(context, ActivitySourceWorkScheduler.GF_INTRADAY_SYNC_WORK_NAME, snapshotId);
            final File backup = new File(snapshot.getPath() + ".bak");
            assertTrue(snapshot.renameTo(backup));

            final List<ActivitySourceWorkScheduler.ScheduledWork> snapshots =
                ActivitySourceWorkScheduler.snapshotWorks(workManager, context);

            assertEquals(1, snapshots.size());
            assertTrue("AtomicFile should recover the previous snapshot from its backup", snapshot.isFile());
        }

        @Test
        public void cancelWorks_deletesAtomicBackupSidecars() throws Exception {
            final Context context = ApplicationProvider.getApplicationContext();
            final WorkManager workManager = WorkManager.getInstance(context);
            final ActivitySourceWorkScheduler scheduler = new ActivitySourceWorkScheduler(workManager,
                "cleanup-user",
                "cleanup-secret",
                "cleanup-api-key",
                "https://cleanup.fjuul.test",
                context);
            scheduler.scheduleGFIntradaySyncWork(Collections.singleton(FitnessMetricsType.INTRADAY_HEART_RATE));
            final String snapshotId =
                snapshotIdForWork(workManager, ActivitySourceWorkScheduler.GF_INTRADAY_SYNC_WORK_NAME);
            final File snapshot =
                snapshotFile(context, ActivitySourceWorkScheduler.GF_INTRADAY_SYNC_WORK_NAME, snapshotId);
            final File backup = new File(snapshot.getPath() + ".bak");
            assertTrue(snapshot.renameTo(backup));

            ActivitySourceWorkScheduler.cancelWorks(workManager, context);

            assertFalse(snapshot.exists());
            assertFalse(backup.exists());
            assertFalse(new File(snapshot.getPath() + ".new").exists());
        }

        private String snapshotIdForWork(WorkManager workManager, String workName) throws Exception {
            return workManager.getWorkInfosForUniqueWork(workName)
                .get()
                .get(0)
                .getTags()
                .stream()
                .filter(tag -> tag.startsWith("com.fjuul.sdk.activity_source_snapshot:"))
                .findFirst()
                .orElseThrow(AssertionError::new)
                .substring("com.fjuul.sdk.activity_source_snapshot:".length());
        }

        private File snapshotFile(Context context, String workName, String snapshotId) {
            return new File(new File(context.getNoBackupFilesDir(), "fjuul_activity_source_work_snapshots"),
                workName + "." + snapshotId + ".snapshot");
        }

        private Operation operationWithFailure(Throwable error) {
            final Operation operation = mock(Operation.class);
            final SettableFuture<Operation.State.SUCCESS> result = SettableFuture.create();
            result.setException(error);
            doAnswer(invocation -> result).when(operation).getResult();
            return operation;
        }

        private Operation operationWithSuccess() {
            final Operation operation = mock(Operation.class);
            final SettableFuture<Operation.State.SUCCESS> result = SettableFuture.create();
            result.set(Operation.SUCCESS);
            doAnswer(invocation -> result).when(operation).getResult();
            return operation;
        }

        @Test
        public void initializeAsync_andDisableBackgroundWorkers_doNotWaitAndReportCompletion() throws Exception {
            final Context context = ApplicationProvider.getApplicationContext();
            final IStorage storage = mock(IStorage.class);
            final CountDownLatch storageReadStarted = new CountDownLatch(1);
            final CountDownLatch allowStorageReadToFinish = new CountDownLatch(1);
            doAnswer(invocation -> {
                storageReadStarted.countDown();
                if (!allowStorageReadToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("test did not release the blocked storage read");
                }
                throw new IllegalStateException("storage unavailable");
            }).when(storage).get(anyString());
            final ApiClient client = mock(ApiClient.class);
            when(client.getAppContext()).thenReturn(context);
            when(client.getStorage()).thenReturn(storage);
            final AtomicReference<Result<Void>> initializationResult = new AtomicReference<>();
            final AtomicReference<Result<Void>> cancellationResult = new AtomicReference<>();
            final AtomicBoolean callbackOnMainThread = new AtomicBoolean(false);
            final AtomicBoolean cancellationCallbackOnMainThread = new AtomicBoolean(false);
            final long startTime = System.nanoTime();

            ActivitySourcesManager.initializeAsync(client, mock(ActivitySourcesManagerConfig.class), result -> {
                initializationResult.set(result);
                callbackOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            });

            assertTrue(storageReadStarted.await(5, TimeUnit.SECONDS));
            assertTrue("initializeAsync must return while background setup is blocked",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) < 1000);
            final long disableStartTime = System.nanoTime();
            ActivitySourcesManager.disableBackgroundWorkersAsync(context, result -> {
                cancellationResult.set(result);
                cancellationCallbackOnMainThread.set(Looper.myLooper() == Looper.getMainLooper());
            });
            assertTrue("logout cancellation must be queued without waiting for the busy executor",
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - disableStartTime) < 1000);

            allowStorageReadToFinish.countDown();
            ActivitySourcesManager.awaitWorkManagerExecutorForTesting();
            shadowOf(getMainLooper()).idle();

            assertTrue(initializationResult.get().isError());
            assertTrue("initialization callback should remain on main", callbackOnMainThread.get());
            assertFalse(cancellationResult.get().isError());
            assertTrue(cancellationCallbackOnMainThread.get());
            assertThrows(IllegalStateException.class, ActivitySourcesManager::getInstance);
        }

        @Test
        public void workerWaitsForPendingAsyncInitializationWithoutHoldingClassMonitor() throws Exception {
            final Context context = ApplicationProvider.getApplicationContext();
            final IStorage storage = mock(IStorage.class);
            final CountDownLatch storageReadStarted = new CountDownLatch(1);
            final CountDownLatch allowStorageReadToFinish = new CountDownLatch(1);
            doAnswer(invocation -> {
                storageReadStarted.countDown();
                if (!allowStorageReadToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("test did not release the blocked storage read");
                }
                throw new IllegalStateException("foreground storage unavailable");
            }).when(storage).get(anyString());
            final ApiClient failingClient = mock(ApiClient.class);
            when(failingClient.getAppContext()).thenReturn(context);
            when(failingClient.getStorage()).thenReturn(storage);
            ActivitySourcesManager
                .initializeAsync(failingClient, ActivitySourcesManagerConfig.buildDefault(), result -> {});
            assertTrue(storageReadStarted.await(5, TimeUnit.SECONDS));

            final Data workerInput = new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, "worker-user")
                .putString(GFSyncWorker.KEY_USER_SECRET_ARG, "worker-secret")
                .putString(GFSyncWorker.KEY_API_KEY_ARG, "worker-key")
                .putString(GFSyncWorker.KEY_BASE_URL_ARG, "https://worker.fjuul.test")
                .build();
            final ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
            final Future<ActivitySourcesManager> workerManager =
                workerExecutor.submit(() -> ActivitySourcesManager.getOrInitializeForWorker(context, workerInput));
            assertFalse(workerManager.isDone());

            allowStorageReadToFinish.countDown();
            final ActivitySourcesManager manager = workerManager.get(5, TimeUnit.SECONDS);
            workerExecutor.shutdownNow();
            assertSame(manager, ActivitySourcesManager.getInstance());
            assertSame(manager, ActivitySourcesManager.getOrInitializeForWorker(context, workerInput));
        }

        @Test
        public void workerInitializationFailureIsPropagatedWithoutRetry() {
            final Context context = mock(Context.class);
            when(context.getApplicationContext()).thenReturn(context);
            final AtomicBoolean storageRequested = new AtomicBoolean(false);
            when(context.getSharedPreferences(anyString(), anyInt())).thenAnswer(invocation -> {
                assertFalse("worker initialization should be attempted only once", storageRequested.getAndSet(true));
                throw new IllegalStateException("storage unavailable");
            });
            final Data workerInput = new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, "worker-user")
                .putString(GFSyncWorker.KEY_USER_SECRET_ARG, "worker-secret")
                .putString(GFSyncWorker.KEY_API_KEY_ARG, "worker-key")
                .putString(GFSyncWorker.KEY_BASE_URL_ARG, "https://worker.fjuul.test")
                .build();

            assertThrows(IllegalStateException.class,
                () -> ActivitySourcesManager.getOrInitializeForWorker(context, workerInput));
            assertTrue(storageRequested.get());
        }

        @Test
        public void cancellationInvalidatesManagerPublishedByQueuedInitialization() throws Exception {
            final Context context = ApplicationProvider.getApplicationContext();
            final IStorage storageA = mock(IStorage.class);
            final ApiClient clientA = mockClientWithStorage(context, "user-a", "secret-a", storageA);
            ActivitySourcesManager.initialize(clientA, ActivitySourcesManagerConfig.buildDefault());
            final ActivitySourcesManager managerA = ActivitySourcesManager.getInstance();
            final CountDownLatch storageReadStarted = new CountDownLatch(1);
            final CountDownLatch allowStorageReadToFinish = new CountDownLatch(1);
            final IStorage storageB = mock(IStorage.class);
            doAnswer(invocation -> {
                storageReadStarted.countDown();
                if (!allowStorageReadToFinish.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("test did not release the initialization read");
                }
                return null;
            }).when(storageB).get(anyString());
            final ApiClient clientB = mockClientWithStorage(context, "user-b", "secret-b", storageB);
            final AtomicReference<Result<Void>> cancellationResult = new AtomicReference<>();

            ActivitySourcesManager.initializeAsync(clientB, ActivitySourcesManagerConfig.buildDefault(), result -> {});
            assertTrue(storageReadStarted.await(5, TimeUnit.SECONDS));
            ActivitySourcesManager.disableBackgroundWorkersAsync(context, cancellationResult::set);
            allowStorageReadToFinish.countDown();
            ActivitySourcesManager.awaitWorkManagerExecutorForTesting();
            shadowOf(getMainLooper()).idle();

            final ActivitySourcesManager managerB =
                ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientB));
            assertNotSame(managerA, managerB);
            assertFalse("queued cancellation must invalidate the replacement manager",
                managerB.automaticRefreshesEnabledForTesting());
            assertFalse(cancellationResult.get().isError());
        }

        private ApiClient mockClientWithStorage(Context context,
            String userToken,
            String userSecret,
            IStorage storage) {
            final ApiClient client = mock(ApiClient.class);
            when(client.getAppContext()).thenReturn(context);
            when(client.getStorage()).thenReturn(storage);
            when(client.getUserToken()).thenReturn(userToken);
            when(client.getUserSecret()).thenReturn(userSecret);
            when(client.getApiKey()).thenReturn("api-key");
            when(client.getBaseUrl()).thenReturn("https://" + userToken + ".fjuul.test");
            when(client.buildSigningClient()).thenReturn(new OkHttpClient());
            return client;
        }

        private Data inputDataOf(ApiClient client) {
            return new Data.Builder().putString(GFSyncWorker.KEY_USER_TOKEN_ARG, client.getUserToken())
                .putString(GFSyncWorker.KEY_USER_SECRET_ARG, client.getUserSecret())
                .putString(GFSyncWorker.KEY_API_KEY_ARG, client.getApiKey())
                .putString(GFSyncWorker.KEY_BASE_URL_ARG, client.getBaseUrl())
                .build();
        }
    }

    public static class StaleRefreshTests extends GivenRobolectricContext {
        final TrackerConnection polarConnection = new TrackerConnection("5f2c8e1a-3b7d-4a9c-b6e2-1d8f4a7c3e90",
            TrackerValue.POLAR.getValue(),
            Date.from(Instant.parse("2020-09-10T10:05:00Z")),
            null);
        final TrackerConnection garminConnection = new TrackerConnection("a3e7c9b1-6d2f-4e8a-9c5b-7f1d3b6e2a48",
            TrackerValue.GARMIN.getValue(),
            Date.from(Instant.parse("2020-09-11T10:05:00Z")),
            null);
        final TrackerConnection healthConnectConnection = new TrackerConnection("c8b4d2e6-1a7f-4c3e-8d9b-5e2a6f1c7d34",
            TrackerValue.HEALTH_CONNECT.getValue(),
            Date.from(Instant.parse("2020-09-12T10:05:00Z")),
            null);

        ActivitySourcesManager subject;
        BackgroundWorkManager mockedBackgroundWorkManager;
        ActivitySourcesService mockedSourcesService;
        ActivitySourcesStateStore mockedStateStore;
        ActivitySourceResolver activitySourceResolver;
        HealthConnectActivitySource mockedHealthConnect;
        Context mockContext;
        MockedStatic<HealthConnectActivitySource> healthConnectStatic;

        @Before
        public void beforeTest() {
            mockContext = mock(Context.class);
            mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
            mockedSourcesService = mock(ActivitySourcesService.class);
            mockedStateStore = mock(ActivitySourcesStateStore.class);
            activitySourceResolver = mock(ActivitySourceResolver.class);
            // Looked up on every state update, even without a Google Fit connection.
            when(activitySourceResolver.getInstanceByTrackerValue(TrackerValue.GOOGLE_FIT.getValue()))
                .thenReturn(mock(GoogleFitActivitySource.class));
            mockedHealthConnect = mock(HealthConnectActivitySource.class);
            when(activitySourceResolver.getInstanceByTrackerValue(TrackerValue.HEALTH_CONNECT.getValue()))
                .thenReturn(mockedHealthConnect);
            healthConnectStatic = mockStatic(HealthConnectActivitySource.class);
            healthConnectStatic.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(mockContext))
                .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
            subject = new ActivitySourcesManager(mock(ActivitySourcesManagerConfig.class),
                mockedBackgroundWorkManager,
                mockedSourcesService,
                mockedStateStore,
                activitySourceResolver,
                new CopyOnWriteArrayList<>(),
                mockContext);
        }

        @After
        public void afterTest() {
            healthConnectStatic.close();
            ActivitySourcesManager.setInstance(null);
        }

        @Test
        public void refreshCurrent_whenOlderResponseArrivesLast_keepsNewerState() {
            final ApiCallCallback<TrackerConnection[]> olderResponse = requestRefresh();
            final ApiCallCallback<TrackerConnection[]> newerResponse = requestRefresh();

            newerResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {garminConnection}));
            olderResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {polarConnection}));
            awaitAsyncManagerWork();

            assertEquals("should keep the newer connections",
                Collections.singletonList(garminConnection.getId()),
                currentConnectionIds());
            verify(mockedStateStore).setConnections(Collections.singletonList(garminConnection));
            verify(mockedStateStore, never()).setConnections(Collections.singletonList(polarConnection));
        }

        @Test
        public void refreshCurrent_whenRequestedBeforeDisconnect_doesNotRestoreDisconnectedConnection() {
            final ApiCallCallback<TrackerConnection[]> initialResponse = requestRefresh();
            initialResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {polarConnection}));
            awaitAsyncManagerWork();
            final ApiCallCallback<TrackerConnection[]> staleResponse = requestRefresh();
            final ActivitySourceConnection sourceConnection =
                new ActivitySourceConnection(polarConnection, PolarActivitySource.getInstance());
            final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                invocation.getArgument(0, ApiCallCallback.class).onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedDisconnectApiCall).enqueue(any());
            when(mockedSourcesService.disconnect(sourceConnection)).thenReturn(mockedDisconnectApiCall);
            subject.disconnect(sourceConnection, mock(Callback.class));
            awaitAsyncManagerWork();

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {polarConnection}));
            awaitAsyncManagerWork();

            assertTrue("should not restore the disconnected connection", currentConnectionIds().isEmpty());
        }

        @Test
        public void disconnect_whenRefreshStartsAfterAcknowledgement_preservesTheNewerRefresh() throws Exception {
            final CountDownLatch queueBlocked = new CountDownLatch(1);
            final CountDownLatch releaseQueue = new CountDownLatch(1);
            ActivitySourcesManager.enqueueWorkManagerTaskForTesting(() -> {
                queueBlocked.countDown();
                awaitLatch(releaseQueue);
            });
            assertTrue(queueBlocked.await(5, TimeUnit.SECONDS));

            final TrackerConnection disconnectedTracker = new TrackerConnection("disconnect_id",
                TrackerValue.POLAR.getValue(),
                Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                null);
            final ActivitySourceConnection disconnectedConnection =
                new ActivitySourceConnection(disconnectedTracker, PolarActivitySource.getInstance());
            final ApiCall<Void> disconnectCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                invocation.getArgument(0, ApiCallCallback.class).onResult(null, ApiCallResult.value(null));
                return null;
            }).when(disconnectCall).enqueue(any());
            when(mockedSourcesService.disconnect(disconnectedConnection)).thenReturn(disconnectCall);

            subject.disconnect(disconnectedConnection, mock(Callback.class));
            final ApiCallCallback<TrackerConnection[]> newerRefresh = requestRefresh();
            newerRefresh.onResult(null, ApiCallResult.value(new TrackerConnection[] {garminConnection}));
            releaseQueue.countDown();
            awaitAsyncManagerWork();

            assertEquals(Collections.singletonList(garminConnection.getId()), currentConnectionIds());
            verify(mockedStateStore).setConnections(Collections.singletonList(garminConnection));
        }

        @Test
        public void refresh_whenInvalidatedDuringWorkConfiguration_keepsSourcesAndConnectionsConsistent()
            throws InterruptedException {
            final CountDownLatch configurationStarted = new CountDownLatch(1);
            final CountDownLatch releaseConfiguration = new CountDownLatch(1);
            doAnswer(invocation -> {
                configurationStarted.countDown();
                awaitLatch(releaseConfiguration);
                return null;
            }).when(mockedBackgroundWorkManager).configureHCProfileSyncWork();
            final ApiCallCallback<TrackerConnection[]> refresh = requestRefresh();

            refresh.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            assertTrue(configurationStarted.await(5, TimeUnit.SECONDS));
            subject.supersedeEarlierRefreshes();
            releaseConfiguration.countDown();
            awaitAsyncManagerWork();

            assertTrue(currentConnectionIds().isEmpty());
            verifyNoInteractions(mockedStateStore);
            verify(mockedHealthConnect, never()).setLowerDateBoundary(any());
        }

        @Test
        public void refreshCurrent_whenRequestedBeforeDisablingBackgroundWorkers_doesNotScheduleWorksAgain() {
            ActivitySourcesManager.setInstance(subject);
            final ApiCallCallback<TrackerConnection[]> staleResponse = requestRefresh();
            ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            awaitAsyncManagerWork();

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            awaitAsyncManagerWork();

            verifyNoInteractions(mockedBackgroundWorkManager);
            verifyNoInteractions(mockedStateStore);
            assertTrue(currentConnectionIds().isEmpty());
        }

        @Test
        public void refreshCurrent_whenInstanceWasReplaced_doesNotTouchSharedState() {
            final ApiCallCallback<TrackerConnection[]> staleResponse = requestRefresh();
            subject.markReplaced();

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            awaitAsyncManagerWork();

            verifyNoInteractions(mockedBackgroundWorkManager);
            verifyNoInteractions(mockedStateStore);
        }

        @Test
        public void initialize_whenItFails_leavesPreviousInstanceUsable() {
            ActivitySourcesManager.setInstance(subject);
            final ApiClient failingClient = mock(ApiClient.class);
            when(failingClient.getStorage()).thenThrow(new IllegalStateException("storage unavailable"));

            assertThrows(IllegalStateException.class,
                () -> ActivitySourcesManager.initializeForTesting(failingClient,
                    mock(ActivitySourcesManagerConfig.class),
                    mock(WorkManager.class),
                    HealthConnectAvailability.SDK_AVAILABLE));

            assertSame("should keep the previous instance", subject, ActivitySourcesManager.getInstance());
            requestRefresh().onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            awaitAsyncManagerWork();
            assertEquals("should still apply refreshes",
                Collections.singletonList(healthConnectConnection.getId()),
                currentConnectionIds());
        }

        @Test
        public void disconnect_whenInstanceWasReplaced_updatesPersistedConnectionsOnly() {
            final ApiCallCallback<TrackerConnection[]> initialResponse = requestRefresh();
            initialResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            subject.markReplaced();
            clearInvocations(mockedBackgroundWorkManager, mockedStateStore);
            final ActivitySourceConnection sourceConnection =
                new ActivitySourceConnection(healthConnectConnection, mock(HealthConnectActivitySource.class));
            final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
            doAnswer(invocation -> {
                invocation.getArgument(0, ApiCallCallback.class).onResult(null, ApiCallResult.value(null));
                return null;
            }).when(mockedDisconnectApiCall).enqueue(any());
            when(mockedSourcesService.disconnect(sourceConnection)).thenReturn(mockedDisconnectApiCall);
            final Callback<Void> mockedCallback = mock(Callback.class);

            subject.disconnect(sourceConnection, mockedCallback);
            awaitAsyncManagerWork();

            verify(mockedStateStore).setConnections(Collections.emptyList());
            verifyNoInteractions(mockedBackgroundWorkManager);
            final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(mockedCallback).onResult(callbackResultCaptor.capture());
            assertFalse("callback should have successful result", callbackResultCaptor.getValue().isError());
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenStartedAfterDisabling_doesNotRefresh() {
            ActivitySourcesManager.setInstance(subject);
            ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            awaitAsyncManagerWork();

            ActivitySourcesManager.refreshCurrentIfUploadRejected(new ApiExceptions.ConflictException("409"), subject);

            verifyNoInteractions(mockedSourcesService, mockedStateStore, mockedBackgroundWorkManager);
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenOriginatingManagerWasReplaced_doesNotRefreshNewSession() {
            subject.markReplaced();
            final ActivitySourcesManager replacement = mock(ActivitySourcesManager.class);
            ActivitySourcesManager.setInstance(replacement);

            ActivitySourcesManager.refreshCurrentIfUploadRejected(new ApiExceptions.ConflictException("409"), subject);

            verifyNoInteractions(mockedSourcesService, mockedStateStore, mockedBackgroundWorkManager, replacement);
        }

        @Test
        public void refreshCurrent_whenStartedAfterDisabling_stillAppliesExplicitRefresh() {
            ActivitySourcesManager.setInstance(subject);
            ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            awaitAsyncManagerWork();

            requestRefresh().onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
            awaitAsyncManagerWork();

            verify(mockedStateStore).setConnections(Collections.singletonList(healthConnectConnection));
            verify(mockedBackgroundWorkManager).configureHCIntradaySyncWorks();
            assertEquals(Collections.singletonList(healthConnectConnection.getId()), currentConnectionIds());
        }

        /** Starts a refresh whose response is delivered by calling the returned callback. */
        private ApiCallCallback<TrackerConnection[]> requestRefresh() {
            final ApiCall<TrackerConnection[]> mockedApiCall = mock(ApiCall.class);
            final AtomicReference<ApiCallCallback<TrackerConnection[]>> pendingResponse = new AtomicReference<>();
            doAnswer(invocation -> {
                pendingResponse.set(invocation.getArgument(0, ApiCallCallback.class));
                return null;
            }).when(mockedApiCall).enqueue(any());
            when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedApiCall);
            subject.refreshCurrent(null);
            return pendingResponse.get();
        }

        private List<String> currentConnectionIds() {
            return subject.getCurrent().stream().map(ActivitySourceConnection::getId).collect(Collectors.toList());
        }

        private static void awaitLatch(CountDownLatch latch) {
            try {
                assertTrue("test latch was not released", latch.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }

    }

    public static class SessionOwnershipTests extends GivenRobolectricContext {
        static final String HC_PROFILE_SYNC_WORK_NAME = "com.fjuul.sdk.background_work.hc_profile_sync";
        final TrackerConnection healthConnectConnection = new TrackerConnection("e4a1c7d9-2b6f-4e3a-9d8c-6f1b3a7e5c20",
            TrackerValue.HEALTH_CONNECT.getValue(),
            Date.from(Instant.parse("2020-09-12T10:05:00Z")),
            null);
        final TrackerConnection polarConnection = new TrackerConnection("7b2e9f4c-1d6a-4c8e-a3f5-9e7d2b4c6a18",
            TrackerValue.POLAR.getValue(),
            Date.from(Instant.parse("2020-09-10T10:05:00Z")),
            null);

        Context context;
        ApiClient clientA;
        ApiClient clientB;

        @Before
        public void beforeTest() {
            context = ApplicationProvider.getApplicationContext();
            WorkManagerTestInitHelper.initializeTestWorkManager(context,
                new Configuration.Builder().setExecutor(new SynchronousExecutor()).build());
            clientA = buildClient("https://a.fjuul.test", "api-key", "user-a", "secret-a");
            clientB = buildClient("https://b.fjuul.test", "api-key", "user-b", "secret-b");
            new ActivitySourcesStateStore(clientA.getStorage())
                .setConnections(Collections.singletonList(healthConnectConnection));
            new ActivitySourcesStateStore(clientB.getStorage())
                .setConnections(Collections.singletonList(healthConnectConnection));
        }

        @After
        public void afterTest() {
            ActivitySourcesManager.setInstance(null);
        }

        @Test
        public void getCurrent_afterReplacement_resolvesTheSourcesOfItsOwnSession() {
            ActivitySourcesManager.initialize(clientA, ActivitySourcesManagerConfig.buildDefault());
            final ActivitySourcesManager managerA = ActivitySourcesManager.getInstance();
            final HealthConnectActivitySource healthConnectA = HealthConnectActivitySource.getInstance();
            assertSame(healthConnectA, healthConnectSourceOf(managerA.getCurrent()));

            ActivitySourcesManager.initialize(clientB, ActivitySourcesManagerConfig.buildDefault());

            final HealthConnectActivitySource healthConnectB = HealthConnectActivitySource.getInstance();
            assertNotSame("should publish the new session's source", healthConnectA, healthConnectB);
            assertSame("the old manager should keep its own source",
                healthConnectA,
                healthConnectSourceOf(managerA.getCurrent()));
            assertSame(healthConnectB, healthConnectSourceOf(ActivitySourcesManager.getInstance().getCurrent()));
        }

        @Test
        public void refreshCurrent_whenOldResponseArrivesAfterReplacement_bringsOnlyTheOldSessionSources() {
            final AtomicReference<ApiCallCallback<TrackerConnection[]>> pendingResponse = new AtomicReference<>();
            final ActivitySourcesManager managerA;
            try (MockedConstruction<ActivitySourcesService> services =
                mockConstruction(ActivitySourcesService.class, (service, construction) -> {
                    final ApiCall<TrackerConnection[]> apiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        pendingResponse.set(invocation.getArgument(0, ApiCallCallback.class));
                        return null;
                    }).when(apiCall).enqueue(any());
                    when(service.getCurrentConnections()).thenReturn(apiCall);
                })) {
                initializeForTesting(clientA,
                    ActivitySourcesManagerConfig.buildDefault(),
                    WorkManager.getInstance(context));
                managerA = ActivitySourcesManager.getInstance();
            }
            final HealthConnectActivitySource healthConnectA = HealthConnectActivitySource.getInstance();
            final AtomicReference<Result<List<ActivitySourceConnection>>> refreshResult = new AtomicReference<>();
            managerA.refreshCurrent(refreshResult::set);
            initializeForTesting(clientB,
                ActivitySourcesManagerConfig.buildDefault(),
                WorkManager.getInstance(context));
            pendingResponse.get()
                .onResult(null,
                    ApiCallResult.value(new TrackerConnection[] {healthConnectConnection, polarConnection}));
            awaitAsyncManagerWork();

            assertNotSame(healthConnectA, HealthConnectActivitySource.getInstance());
            assertSame("the late callback should bring the old session's source",
                healthConnectA,
                healthConnectSourceOf(refreshResult.get().getValue()));
            assertEquals("the replaced manager should not apply the late response",
                Collections.singletonList(healthConnectConnection.getId()),
                new ActivitySourcesStateStore(clientA.getStorage()).getConnections()
                    .stream()
                    .map(TrackerConnection::getId)
                    .collect(Collectors.toList()));
        }

        @Test
        public void initialize_whenItFailsAfterCreatingTheSources_keepsThePreviousSessionPublished() {
            ActivitySourcesManager.initialize(clientA, ActivitySourcesManagerConfig.buildDefault());
            final ActivitySourcesManager managerA = ActivitySourcesManager.getInstance();
            final GoogleFitActivitySource googleFitA = GoogleFitActivitySource.getInstance();
            final HealthConnectActivitySource healthConnectA = HealthConnectActivitySource.getInstance();

            try (MockedStatic<HealthConnectActivitySource> healthConnectStatic =
                mockStatic(HealthConnectActivitySource.class, CALLS_REAL_METHODS)) {
                final WorkManager failingWorkManager = mock(WorkManager.class);
                when(failingWorkManager.getWorkInfosForUniqueWork(anyString()))
                    .thenThrow(new IllegalStateException("WorkManager unavailable"));

                assertThrows(IllegalStateException.class,
                    () -> ActivitySourcesManager.initializeForTesting(clientB,
                        ActivitySourcesManagerConfig.buildDefault(),
                        failingWorkManager,
                        HealthConnectAvailability.SDK_AVAILABLE));

                healthConnectStatic
                    .verify(() -> HealthConnectActivitySource.forInternalUseOnly_create(any(), any(), any()));
            }

            assertSame(managerA, ActivitySourcesManager.getInstance());
            assertSame(googleFitA, GoogleFitActivitySource.getInstance());
            assertSame(healthConnectA, HealthConnectActivitySource.getInstance());
            assertSame(healthConnectA, healthConnectSourceOf(managerA.getCurrent()));
            assertSame("the previous manager should still serve its session's workers",
                managerA,
                ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientA)));
        }

        @Test
        public void initialize_whenConfiguringBackgroundWorksFails_restoresThePreviousSession() {
            final WorkManager workManager = spy(WorkManager.getInstance(context));
            final AtomicBoolean failHealthConnectIntradayScheduling = new AtomicBoolean(false);
            doAnswer(invocation -> {
                if (ActivitySourceWorkScheduler.HC_INTRADAY_SYNC_WORK_NAME.equals(invocation.getArgument(0))
                    && failHealthConnectIntradayScheduling.getAndSet(false)) {
                    throw new IllegalStateException("WorkManager database unavailable");
                }
                return invocation.callRealMethod();
            }).when(workManager).enqueueUniquePeriodicWork(anyString(), any(), any(PeriodicWorkRequest.class));
            final ActivitySourcesManager managerA;
            final GoogleFitActivitySource googleFitA;
            final HealthConnectActivitySource healthConnectA;
            try (MockedStatic<HealthConnectActivitySource> healthConnectStatic =
                mockStatic(HealthConnectActivitySource.class, CALLS_REAL_METHODS)) {
                healthConnectStatic.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(any()))
                    .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);
                initializeForTesting(clientA, ActivitySourcesManagerConfig.buildDefault(), workManager);
                managerA = ActivitySourcesManager.getInstance();
                googleFitA = GoogleFitActivitySource.getInstance();
                healthConnectA = HealthConnectActivitySource.getInstance();
                failHealthConnectIntradayScheduling.set(true);
                clearInvocations(workManager);

                assertThrows(IllegalStateException.class,
                    () -> initializeForTesting(clientB, ActivitySourcesManagerConfig.buildDefault(), workManager));
            }

            assertSame(managerA, ActivitySourcesManager.getInstance());
            assertSame(googleFitA, GoogleFitActivitySource.getInstance());
            assertSame(healthConnectA, HealthConnectActivitySource.getInstance());
            assertSame(healthConnectA, healthConnectSourceOf(managerA.getCurrent()));
            final ArgumentCaptor<PeriodicWorkRequest> profileWorkRequests =
                ArgumentCaptor.forClass(PeriodicWorkRequest.class);
            verify(workManager, atLeastOnce())
                .enqueueUniquePeriodicWork(eq(HC_PROFILE_SYNC_WORK_NAME), any(), profileWorkRequests.capture());
            final List<String> profileWorkUsers = profileWorkRequests.getAllValues()
                .stream()
                .map(request -> request.getWorkSpec().input.getString(GFSyncWorker.KEY_USER_TOKEN_ARG))
                .collect(Collectors.toList());
            assertEquals("the failed session's work should be replaced by the previous session's one",
                Arrays.asList("user-b", "user-a"),
                profileWorkUsers);
        }

        @Test
        public void initialize_whenOperationFails_restoresColdStartWorkInputsAndSchedule() throws Exception {
            final WorkManager workManager = spy(WorkManager.getInstance(context));
            final AtomicBoolean failIntradayEnqueue = new AtomicBoolean(false);
            final AtomicBoolean failedOperationReturned = new AtomicBoolean(false);
            final List<PeriodicWorkRequest> enqueuedRequests = new ArrayList<>();
            final Operation failedOperation = mock(Operation.class);
            final SettableFuture<Void> failedResult = SettableFuture.create();
            failedResult.setException(new IllegalStateException("asynchronous WorkManager failure"));
            doAnswer(invocation -> failedResult).when(failedOperation).getResult();
            doAnswer(invocation -> {
                final String workName = invocation.getArgument(0);
                final PeriodicWorkRequest request = invocation.getArgument(2);
                enqueuedRequests.add(request);
                if (ActivitySourceWorkScheduler.HC_INTRADAY_SYNC_WORK_NAME.equals(workName)
                    && failIntradayEnqueue.getAndSet(false)) {
                    failedOperationReturned.set(true);
                    return failedOperation;
                }
                return invocation.callRealMethod();
            }).when(workManager).enqueueUniquePeriodicWork(anyString(), any(), any(PeriodicWorkRequest.class));

            final ActivitySourcesManager coldStartManager;
            final PeriodicWorkRequest originalProfileRequest;
            final GoogleFitActivitySource originalGoogleFitSource;
            final HealthConnectActivitySource originalHealthConnectSource;
            try (MockedStatic<HealthConnectActivitySource> healthConnectStatic =
                mockStatic(HealthConnectActivitySource.class, CALLS_REAL_METHODS)) {
                healthConnectStatic.when(() -> HealthConnectActivitySource.getHealthConnectAvailability(any()))
                    .thenReturn(HealthConnectAvailability.SDK_AVAILABLE);

                initializeForTesting(clientA, ActivitySourcesManagerConfig.buildDefault(), workManager);
                originalProfileRequest = enqueuedRequests.stream()
                    .filter(request -> request.getWorkSpec().workerClassName
                        .equals("com.fjuul.sdk.activitysources.workers.HCProfileSyncWorker"))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
                final String inputSnapshotTag = originalProfileRequest.getTags()
                    .stream()
                    .filter(tag -> tag.startsWith("com.fjuul.sdk.activity_source_snapshot:"))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
                assertTrue(inputSnapshotTag.matches("com\\.fjuul\\.sdk\\.activity_source_snapshot:[0-9a-f-]{36}"));
                assertFalse(inputSnapshotTag.contains("user-a"));
                assertFalse(inputSnapshotTag.contains("secret-a"));
                assertFalse(inputSnapshotTag.contains("api-key"));
                final String inputSnapshotId =
                    inputSnapshotTag.substring("com.fjuul.sdk.activity_source_snapshot:".length());
                final File snapshotFile =
                    new File(new File(context.getNoBackupFilesDir(), "fjuul_activity_source_work_snapshots"),
                        HC_PROFILE_SYNC_WORK_NAME + "." + inputSnapshotId + ".snapshot");
                assertTrue("credential-bearing snapshot should be stored outside backup", snapshotFile.isFile());

                ActivitySourcesManager.setInstance(null);
                coldStartManager = ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientA));
                assertSame(coldStartManager, ActivitySourcesManager.getInstance());
                assertFalse(
                    "cold-start worker initialization should leave the prior tagged job discoverable: "
                        + workManager.getWorkInfosForUniqueWork(HC_PROFILE_SYNC_WORK_NAME).get(),
                    ActivitySourceWorkScheduler.snapshotWorks(workManager, context).isEmpty());
                originalGoogleFitSource = GoogleFitActivitySource.getInstance();
                originalHealthConnectSource = HealthConnectActivitySource.getInstance();
                enqueuedRequests.clear();
                failIntradayEnqueue.set(true);

                assertThrows(IllegalStateException.class,
                    () -> initializeForTesting(clientB, ActivitySourcesManagerConfig.buildDefault(), workManager));
                assertTrue("the failed WorkManager Operation should be exercised", failedOperationReturned.get());
            }

            assertSame("the cold-start manager should remain active",
                coldStartManager,
                ActivitySourcesManager.getInstance());
            assertSame(originalGoogleFitSource, GoogleFitActivitySource.getInstance());
            assertSame(originalHealthConnectSource, HealthConnectActivitySource.getInstance());
            final List<PeriodicWorkRequest> profileRequests = enqueuedRequests.stream()
                .filter(request -> request.getWorkSpec().workerClassName
                    .equals("com.fjuul.sdk.activitysources.workers.HCProfileSyncWorker"))
                .collect(Collectors.toList());
            assertEquals("candidate setup and rollback should both enqueue profile work for user-b then user-a",
                Arrays.asList("user-b", "user-a"),
                profileRequests.stream()
                    .map(request -> request.getWorkSpec().input.getString(GFSyncWorker.KEY_USER_TOKEN_ARG))
                    .collect(Collectors.toList()));
            assertEquals("rollback should restore the old user's credentials",
                "user-a",
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().input
                    .getString(GFSyncWorker.KEY_USER_TOKEN_ARG));
            assertEquals("rollback should restore the previous profile metrics",
                originalProfileRequest.getWorkSpec().input.getStringArray(HCProfileSyncWorker.KEY_HC_PROFILE_METRICS),
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().input
                    .getStringArray(HCProfileSyncWorker.KEY_HC_PROFILE_METRICS));
            assertEquals(originalProfileRequest.getWorkSpec().initialDelay,
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().initialDelay);
            assertEquals(originalProfileRequest.getWorkSpec().intervalDuration,
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().intervalDuration);
            assertEquals(originalProfileRequest.getWorkSpec().flexDuration,
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().flexDuration);
            assertEquals(originalProfileRequest.getWorkSpec().constraints,
                profileRequests.get(profileRequests.size() - 1).getWorkSpec().constraints);
        }

        @Test
        public void getOrInitializeForWorker_whenQueuedByAnotherSession_returnsNullAndKeepsTheInstance() {
            ActivitySourcesManager.initialize(clientA, ActivitySourcesManagerConfig.buildDefault());
            final ActivitySourcesManager managerA = ActivitySourcesManager.getInstance();
            final List<Data> foreignInputs = Arrays.asList(inputDataOf(clientB),
                inputData("https://a.fjuul.test", "api-key", "user-b", "secret-a"),
                inputData("https://b.fjuul.test", "api-key", "user-a", "secret-a"),
                inputData("https://a.fjuul.test", "other-api-key", "user-a", "secret-a"),
                inputData("https://a.fjuul.test", "api-key", "user-a", "other-secret"),
                new Data.Builder().build());

            for (final Data foreignInput : foreignInputs) {
                assertNull(ActivitySourcesManager.getOrInitializeForWorker(context, foreignInput));
                assertSame(managerA, ActivitySourcesManager.getInstance());
            }
            assertSame(managerA, ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientA)));
        }

        @Test
        public void healthConnectWorker_whenQueuedByAnotherSession_finishesWithoutSyncing() throws Exception {
            final ActivitySourcesManager managerA = spy(buildManager(clientA));
            ActivitySourcesManager.setInstance(managerA);

            final ListenableWorker.Result result = runHealthConnectIntradayWorker(inputDataOf(clientB));

            assertEquals(ListenableWorker.Result.success(), result);
            verify(managerA, never()).getCurrent();
            assertSame("should not replace the foreground manager", managerA, ActivitySourcesManager.getInstance());
        }

        @Test
        public void healthConnectWorker_whenQueuedByTheActiveSession_syncsThroughIt() throws Exception {
            final ActivitySourcesManager managerA = spy(buildManager(clientA));
            ActivitySourcesManager.setInstance(managerA);

            final ListenableWorker.Result result = runHealthConnectIntradayWorker(inputDataOf(clientA));

            assertEquals(ListenableWorker.Result.success(), result);
            verify(managerA).getCurrent();
            assertSame(managerA, ActivitySourcesManager.getInstance());
        }

        @Test
        public void healthConnectWorker_whenNoActiveManager_initializesOneForItsSession() throws Exception {
            final ApiClient clientC = buildClient("https://c.fjuul.test", "api-key", "user-c", "secret-c");

            final ListenableWorker.Result result = runHealthConnectIntradayWorker(inputDataOf(clientC));

            assertEquals(ListenableWorker.Result.success(), result);
            final ActivitySourcesManager manager = ActivitySourcesManager.getInstance();
            assertSame(manager, ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientC)));
            assertNull(ActivitySourcesManager.getOrInitializeForWorker(context, inputDataOf(clientA)));
        }

        @Test
        public void workerInitializationConfig_keepsScheduledWorkUntouched() {
            final ActivitySourcesManagerConfig config = ActivitySourcesManager.workerInitializationConfig();

            assertEquals(
                Arrays.asList(ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED,
                    ActivitySourcesManagerConfig.BackgroundSyncMode.UNTOUCHED),
                Arrays.asList(config.getGoogleFitIntradayBackgroundSyncMode(),
                    config.getGoogleFitSessionsBackgroundSyncMode(),
                    config.getProfileBackgroundSyncMode(),
                    config.getHealthConnectIntradayBackgroundSyncMode(),
                    config.getHealthConnectDailyBackgroundSyncMode(),
                    config.getHealthConnectProfileBackgroundSyncMode()));
            assertTrue(config.getCollectableFitnessMetrics().isEmpty());
        }

        private ApiClient buildClient(String baseUrl, String apiKey, String userToken, String userSecret) {
            return new ApiClient.Builder(context, baseUrl, apiKey)
                .setUserCredentials(new UserCredentials(userToken, userSecret))
                .build();
        }

        private void initializeForTesting(ApiClient client,
            ActivitySourcesManagerConfig config,
            WorkManager workManager) {
            ActivitySourcesManager.initializeForTesting(client,
                config,
                workManager,
                HealthConnectActivitySource.getHealthConnectAvailability(context));
        }

        private ActivitySourcesManager buildManager(ApiClient client) {
            return new ActivitySourcesManager(mock(ActivitySourcesManagerConfig.class),
                mock(BackgroundWorkManager.class),
                mock(ActivitySourcesService.class),
                mock(ActivitySourcesStateStore.class),
                mock(ActivitySourceResolver.class),
                new CopyOnWriteArrayList<>(),
                context,
                client);
        }

        private ListenableWorker.Result runHealthConnectIntradayWorker(Data inputData) throws Exception {
            final HCIntradaySyncWorker worker =
                TestListenableWorkerBuilder.from(context, HCIntradaySyncWorker.class).setInputData(inputData).build();
            return worker.startWork().get(5, TimeUnit.SECONDS);
        }

        private static Data inputDataOf(ApiClient client) {
            return inputData(client.getBaseUrl(), client.getApiKey(), client.getUserToken(), client.getUserSecret());
        }

        private static Data inputData(String baseUrl, String apiKey, String userToken, String userSecret) {
            return new Data.Builder().putString(GFSyncWorker.KEY_BASE_URL_ARG, baseUrl)
                .putString(GFSyncWorker.KEY_API_KEY_ARG, apiKey)
                .putString(GFSyncWorker.KEY_USER_TOKEN_ARG, userToken)
                .putString(GFSyncWorker.KEY_USER_SECRET_ARG, userSecret)
                .build();
        }

        private static ActivitySource healthConnectSourceOf(List<ActivitySourceConnection> connections) {
            return connections.stream()
                .filter(connection -> TrackerValue.HEALTH_CONNECT.getValue().equals(connection.getTracker()))
                .findFirst()
                .orElseThrow(AssertionError::new)
                .getActivitySource();
        }

        private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (thread.getState() != state) {
                if (System.nanoTime() > deadline) {
                    throw new AssertionError("thread did not reach " + state);
                }
                Thread.sleep(1);
            }
        }
    }
}
