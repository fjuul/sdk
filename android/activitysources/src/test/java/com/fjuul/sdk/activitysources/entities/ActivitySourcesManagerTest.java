package com.fjuul.sdk.activitysources.entities;

import android.content.Intent;
import android.os.Build;

import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.google.android.gms.tasks.Tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.emory.mathcs.backport.java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class ActivitySourcesManagerTest {

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    public static ActivitySourceResolver mockActivitySourceResolverWithDefaultBehavior() {
        final ActivitySourceResolver mockedResolver = mock(ActivitySourceResolver.class);
//        final commonActivitySource
//        when(resolver.getInstanceByTrackerValue());
        return mockedResolver;
    }
    @RunWith(Enclosed.class)
    public static class InstanceMethods {
        public static class ConnectTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedActivitySourcesService;
            ActivitySourcesStateStore mockedStateStore;
            List<TrackerConnection> trackerConnections;

            @Before
            public void beforeTest() {
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                final ActivitySourceResolver activitySourceResolver = new ActivitySourceResolver();
                trackerConnections = Collections.emptyList();
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedActivitySourcesService, mockedStateStore, activitySourceResolver, trackerConnections);
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

                final ExternalAuthenticationFlowRequired mockedConnectionResult = mock(ExternalAuthenticationFlowRequired.class);
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

                final ApiExceptions.BadRequestException apiCallException = new ApiExceptions.BadRequestException("Bad request");
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

            @Before
            public void beforeTest() {
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                activitySourceResolver = new ActivitySourceResolver();
            }

            @Test
            public void disconnect_whenGoogleFit_disconnectsAndRefreshesCurrentConnections() {
                final GoogleFitActivitySource googleFit = mock(GoogleFitActivitySource.class);
                final TrackerConnection gfTrackerConnection = new TrackerConnection("gf_c_id",
                    ActivitySource.TrackerValue.GOOGLE_FIT.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);
                final ActivitySourceConnection gfConnection = new ActivitySourceConnection(gfTrackerConnection, googleFit);
                final List<TrackerConnection> trackerConnections = Stream.of(gfTrackerConnection)
                    .collect(Collectors.toList());
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, activitySourceResolver, trackerConnections);
                final Callback<List<ActivitySourceConnection>> mockedCallback = mock(Callback.class);
                when(googleFit.disable()).thenReturn(Tasks.forResult(null));
                final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(null));
                    return null;
                }).when(mockedDisconnectApiCall).enqueue(any());
                when(mockedSourcesService.disconnect(gfConnection)).thenReturn(mockedDisconnectApiCall);


                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(new TrackerConnection[0]));
                    return null;
                }).when(mockedGetConnectionsApiCall).enqueue(any());
                when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);

                subject.disconnect(gfConnection, mockedCallback);

                // should revoke GoogleFit OAuth permissions
                verify(googleFit).disable();
                // should ask the sources service to disconnect
                verify(mockedSourcesService).disconnect(gfConnection);
                verify(mockedDisconnectApiCall).enqueue(any());
                // should ask the sources service to get fresh ones
                verify(mockedSourcesService).getCurrentConnections();
                verify(mockedGetConnectionsApiCall).enqueue(any());
                // should pass new connections to the activity sources state store
                verify(mockedStateStore).setConnections(Collections.emptyList());
                assertEquals("should set new connections in the subject",
                    Collections.emptyList(),
                    subject.getCurrent());
                final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<List<ActivitySourceConnection>> callbackResult = callbackResultCaptor.getValue();
                assertFalse("callback should have successful result", callbackResult.isError());
                assertEquals("callback result should have new connections",
                    Collections.emptyList(),
                    callbackResult.getValue());
            }

            @Test
            public void disconnect_whenExternalActivitySource_disconnectsAndRefreshesCurrentConnections() {
                final FitbitActivitySource fitbit = FitbitActivitySource.getInstance();
                final TrackerConnection fitbitTrackerConnection = new TrackerConnection("fitbit_c_id",
                    ActivitySource.TrackerValue.FITBIT.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);
                final ActivitySourceConnection fitbitConnection = new ActivitySourceConnection(fitbitTrackerConnection, fitbit);
                final List<TrackerConnection> trackerConnections = Stream.of(fitbitTrackerConnection)
                    .collect(Collectors.toList());
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, activitySourceResolver, trackerConnections);
                final Callback<List<ActivitySourceConnection>> mockedCallback = mock(Callback.class);
                final ApiCall<Void> mockedDisconnectApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(null));
                    return null;
                }).when(mockedDisconnectApiCall).enqueue(any());
                when(mockedSourcesService.disconnect(fitbitConnection)).thenReturn(mockedDisconnectApiCall);

                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(new TrackerConnection[0]));
                    return null;
                }).when(mockedGetConnectionsApiCall).enqueue(any());
                when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);

                subject.disconnect(fitbitConnection, mockedCallback);

                // should ask the sources service to disconnect
                verify(mockedSourcesService).disconnect(fitbitConnection);
                verify(mockedDisconnectApiCall).enqueue(any());
                // should ask the sources service to get fresh ones
                verify(mockedSourcesService).getCurrentConnections();
                verify(mockedGetConnectionsApiCall).enqueue(any());
                // should pass new connections to the activity sources state store
                verify(mockedStateStore).setConnections(Collections.emptyList());
                assertEquals("should set new connections in the subject",
                    Collections.emptyList(),
                    subject.getCurrent());
                final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<List<ActivitySourceConnection>> callbackResult = callbackResultCaptor.getValue();
                assertFalse("callback should have successful result", callbackResult.isError());
                assertEquals("callback result should have new connections",
                    Collections.emptyList(),
                    callbackResult.getValue());
            }
        }

        public static class GetCurrentTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedSourcesService;
            ActivitySourcesStateStore mockedStateStore;
            ActivitySourceResolver activitySourceResolver;

            @Before
            public void beforeTest() {
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                activitySourceResolver = new ActivitySourceResolver();
            }

            @Test
            public void getCurrent_whenNoCurrentConnections_returnsNull() {
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, activitySourceResolver, null);
                assertNull(subject.getCurrent());
            }

            @Test
            public void getCurrent_withExistedTrackerConnections_returnsActivitySourceConnections() {
                final TrackerConnection fitbitTrackerConnection = new TrackerConnection("fitbit_c_id",
                    ActivitySource.TrackerValue.FITBIT.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);
                final List<TrackerConnection> trackerConnections = Stream.of(fitbitTrackerConnection).collect(Collectors.toList());
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, activitySourceResolver, trackerConnections);
                final List<ActivitySourceConnection> activitySourceConnections = subject.getCurrent();
                assertEquals("should have 1 activity source connection", 1, activitySourceConnections.size());
                ActivitySourceConnection fitbitActivitySourceConnection = activitySourceConnections.get(0);
                assertEquals("activity source connection should have fitbit activity source",
                    FitbitActivitySource.getInstance(),
                    fitbitActivitySourceConnection.getActivitySource());
                assertEquals("activity source connection should have fitbit tracker",
                    fitbitTrackerConnection.getId(),
                    fitbitActivitySourceConnection.getId());
            }
        }

        public static class RefreshCurrentTests extends GivenRobolectricContext {
            ActivitySourcesManager subject;
            ActivitySourcesManagerConfig mockedConfig;
            BackgroundWorkManager mockedBackgroundWorkManager;
            ActivitySourcesService mockedSourcesService;
            ActivitySourcesStateStore mockedStateStore;
            ActivitySourceResolver mockedActivitySourceResolver;

            @Before
            public void beforeTest() {
                mockedConfig = mock(ActivitySourcesManagerConfig.class);
                mockedBackgroundWorkManager = mock(BackgroundWorkManager.class);
                mockedSourcesService = mock(ActivitySourcesService.class);
                mockedStateStore = mock(ActivitySourcesStateStore.class);
                mockedActivitySourceResolver = mock(ActivitySourceResolver.class);
            }

            @Test
            public void refreshCurrent_whenGetNewConnectionsWithGoogleFitAndCallbackIsNull_refreshesCurrentConnections() {
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, mockedActivitySourceResolver, null);

                final TrackerConnection gfTrackerConnection = new TrackerConnection("gf_c_id",
                    ActivitySource.TrackerValue.GOOGLE_FIT.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);

                final TrackerConnection[] newConnections = new TrackerConnection[] { gfTrackerConnection };
                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(newConnections));
                    return null;
                }).when(mockedGetConnectionsApiCall).enqueue(any());
                when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
                GoogleFitActivitySource googleFitStub = mock(GoogleFitActivitySource.class);
                when(mockedActivitySourceResolver.getInstanceByTrackerValue(any())).thenReturn(googleFitStub);

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
                verify(mockedBackgroundWorkManager).configureBackgroundGFSyncWorks();
            }

            @Test
            public void refreshCurrent_whenGetNewConnectionsWithPolarAndCallbackIsNotNull_refreshesCurrentConnections() {
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, mockedActivitySourceResolver, null);

                final TrackerConnection polarTrackerConnection = new TrackerConnection("polar_c_id",
                    ActivitySource.TrackerValue.POLAR.getValue(),
                    Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                    null);

                final TrackerConnection[] newConnections = new TrackerConnection[] { polarTrackerConnection };
                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback = invocation.getArgument(0, ApiCallCallback.class);
                    callback.onResult(null, ApiCallResult.value(newConnections));
                    return null;
                }).when(mockedGetConnectionsApiCall).enqueue(any());
                when(mockedSourcesService.getCurrentConnections()).thenReturn(mockedGetConnectionsApiCall);
                final PolarActivitySource polarStub = mock(PolarActivitySource.class);
                when(mockedActivitySourceResolver.getInstanceByTrackerValue(any())).thenReturn(polarStub);
                final Callback<List<ActivitySourceConnection>> mockedCallback = mock(Callback.class);

                subject.refreshCurrent(mockedCallback);

                // should ask the sources service to get fresh ones
                verify(mockedSourcesService).getCurrentConnections();
                verify(mockedGetConnectionsApiCall).enqueue(any());
                // should pass new connections to the activity sources state store
                verify(mockedStateStore).setConnections(Arrays.asList(newConnections));
                final List<ActivitySourceConnection> currentActivitySourceConnections = subject.getCurrent();
                assertEquals("current connections should have 1 entry", 1, currentActivitySourceConnections.size());
                final ActivitySourceConnection polarActivitySourceConnection = currentActivitySourceConnections.get(0);
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
                verify(mockedBackgroundWorkManager).cancelBackgroundGFSyncWorks();
                final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
                // should pass new connections to the callback
                verify(mockedCallback).onResult(callbackResultArgumentCaptor.capture());
                final Result<List<ActivitySourceConnection>> callbackResult = callbackResultArgumentCaptor.getValue();
                assertFalse("callback should have successful result", callbackResult.isError());
                assertEquals("callback result should have polar",
                    polarTrackerConnection.getId(),
                    callbackResult.getValue().get(0).getId());
            }

            @Test
            public void refreshCurrent_whenApiCallFailsAndCallbackIsNotNull_bringsApiCallExceptionToCallback() {
                subject = new ActivitySourcesManager(mockedConfig, mockedBackgroundWorkManager, mockedSourcesService, mockedStateStore, mockedActivitySourceResolver, null);

                final ApiExceptions.BadRequestException apiCallException = new ApiExceptions.BadRequestException("Bad request");
                final ApiCall<TrackerConnection[]> mockedGetConnectionsApiCall = mock(ApiCall.class);
                doAnswer(invocation -> {
                    final ApiCallCallback<TrackerConnection[]> callback = invocation.getArgument(0, ApiCallCallback.class);
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
                final ArgumentCaptor<Result<List<ActivitySourceConnection>>> callbackResultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultArgumentCaptor.capture());
                final Result<List<ActivitySourceConnection>> callbackResult = callbackResultArgumentCaptor.getValue();
                assertTrue("callback should have unsuccessful result", callbackResult.isError());
                assertEquals("callback result should have the api call exception",
                    apiCallException,
                    callbackResult.getError());
            }
        }
    }
}