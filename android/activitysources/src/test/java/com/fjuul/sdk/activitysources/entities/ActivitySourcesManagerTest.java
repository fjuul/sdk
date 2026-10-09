package com.fjuul.sdk.activitysources.entities;

import static android.os.Looper.getMainLooper;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourceResolver;
import com.fjuul.sdk.activitysources.entities.internal.ActivitySourcesStateStore;
import com.fjuul.sdk.activitysources.entities.internal.BackgroundWorkManager;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.ApiClient;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.test.LoggableTestSuite;
import com.google.android.gms.tasks.Tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.work.WorkManager;

@RunWith(Enclosed.class)
public class ActivitySourcesManagerTest {

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext extends LoggableTestSuite {}

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
                    verify(mockedBackgroundWorkManager).cancelGFSyncWorks();
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
                    verify(mockedBackgroundWorkManager).cancelGFSyncWorks();
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
        Context mockContext;
        MockedStatic<HealthConnectActivitySource> healthConnectStatic;

        @Before
        public void beforeTest() {
            mockContext = mock(Context.class);
            mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
            mockedSourcesService = mock(ActivitySourcesService.class);
            mockedStateStore = mock(ActivitySourcesStateStore.class);
            final ActivitySourceResolver activitySourceResolver = mock(ActivitySourceResolver.class);
            // Looked up on every state update, even without a Google Fit connection.
            when(activitySourceResolver.getInstanceByTrackerValue(TrackerValue.GOOGLE_FIT.getValue()))
                .thenReturn(mock(GoogleFitActivitySource.class));
            when(activitySourceResolver.getInstanceByTrackerValue(TrackerValue.HEALTH_CONNECT.getValue()))
                .thenReturn(mock(HealthConnectActivitySource.class));
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

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {polarConnection}));

            assertTrue("should not restore the disconnected connection", currentConnectionIds().isEmpty());
        }

        @Test
        public void refreshCurrent_whenRequestedBeforeDisablingBackgroundWorkers_doesNotScheduleWorksAgain() {
            ActivitySourcesManager.setInstance(subject);
            final ApiCallCallback<TrackerConnection[]> staleResponse = requestRefresh();
            try (MockedStatic<WorkManager> workManagerStatic = mockStatic(WorkManager.class)) {
                workManagerStatic.when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mock(WorkManager.class));
                ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            }

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));

            verifyNoInteractions(mockedBackgroundWorkManager);
            verifyNoInteractions(mockedStateStore);
            assertTrue(currentConnectionIds().isEmpty());
        }

        @Test
        public void refreshCurrent_whenInstanceWasReplaced_doesNotTouchSharedState() {
            final ApiCallCallback<TrackerConnection[]> staleResponse = requestRefresh();
            subject.markReplaced();

            staleResponse.onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));

            verifyNoInteractions(mockedBackgroundWorkManager);
            verifyNoInteractions(mockedStateStore);
        }

        @Test
        public void initialize_whenItFails_leavesPreviousInstanceUsable() {
            ActivitySourcesManager.setInstance(subject);
            final ApiClient failingClient = mock(ApiClient.class);
            when(failingClient.getStorage()).thenThrow(new IllegalStateException("storage unavailable"));

            assertThrows(IllegalStateException.class,
                () -> ActivitySourcesManager.initialize(failingClient, mock(ActivitySourcesManagerConfig.class)));

            assertSame("should keep the previous instance", subject, ActivitySourcesManager.getInstance());
            requestRefresh().onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));
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

            verify(mockedStateStore).setConnections(Collections.emptyList());
            verifyNoInteractions(mockedBackgroundWorkManager);
            final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
            verify(mockedCallback).onResult(callbackResultCaptor.capture());
            assertFalse("callback should have successful result", callbackResultCaptor.getValue().isError());
        }

        @Test
        public void refreshCurrentIfUploadRejected_whenStartedAfterDisabling_doesNotRefresh() {
            ActivitySourcesManager.setInstance(subject);
            try (MockedStatic<WorkManager> workManagerStatic = mockStatic(WorkManager.class)) {
                workManagerStatic.when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mock(WorkManager.class));
                ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            }

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
            try (MockedStatic<WorkManager> workManagerStatic = mockStatic(WorkManager.class)) {
                workManagerStatic.when(() -> WorkManager.getInstance(any(Context.class)))
                    .thenReturn(mock(WorkManager.class));
                ActivitySourcesManager.disableBackgroundWorkers(mockContext);
            }

            requestRefresh().onResult(null, ApiCallResult.value(new TrackerConnection[] {healthConnectConnection}));

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
    }
}
