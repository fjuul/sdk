package com.fjuul.sdk.activitysources.entities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GoogleFitActivitySourceTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    static final String DUMMY_SERVER_CLIENT_ID = "google_server_client_id";

    @RunWith(Enclosed.class)
    public static class InstanceMethods {
        public static class AreFitnessPermissionsGrantedTests extends GivenRobolectricContext {
            GoogleFitActivitySource subject;
            ActivitySourcesService mockedActivitySourcesService;
            Context context;
            GoogleFitDataManagerBuilder mockedGfDataManagerBuilder;

            @Before
            public void beforeTest() {
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
            }

            @Test
            public void areFitnessPermissionsGranted_whenRequireOfflineAccessAndPermissionsWereGranted_returnsTrue() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(true,
                    DUMMY_SERVER_CLIENT_ID,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ),
                            new Scope(Scopes.PROFILE),
                            new Scope(Scopes.OPEN_ID)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);
                    assertTrue("should return true", subject.areFitnessPermissionsGranted());
                }
            }

            @Test
            public void areFitnessPermissionsGranted_whenRequireOfflineAccessAndNotAllPermissionsWereGranted_returnsTrue() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(true,
                    DUMMY_SERVER_CLIENT_ID,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);
                    assertFalse("should return false", subject.areFitnessPermissionsGranted());
                }
            }

            @Test
            public void areFitnessPermissionsGranted_whenNoOfflineAccessRequirementAndPermissionsWereGranted_returnsTrue() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(false,
                    null,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    // NOTE: here is not a scope with profile
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);
                    assertTrue("should return true", subject.areFitnessPermissionsGranted());
                }
            }

            @Test
            public void areFitnessPermissionsGranted_whenNoOfflineAccessRequirementButNotAllPermissionsWereGranted_returnsTrue() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(false,
                    null,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    // NOTE: here is not a scope with profile
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_BODY_READ)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);
                    assertFalse("should return false", subject.areFitnessPermissionsGranted());
                }
            }
        }

        public static class HandleGoogleSignInResultTests extends GivenRobolectricContext {
            GoogleFitActivitySource subject;
            ActivitySourcesService mockedActivitySourcesService;
            Context context;
            GoogleFitDataManagerBuilder mockedGfDataManagerBuilder;

            @Before
            public void beforeTest() {
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
            }

            @Test
            public void handleGoogleSignInResult_whenWrongIntentComes_bringsApiExceptionResultToCallback() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(false,
                    null,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder);

                final Intent testDummyIntent = new Intent();
                final Callback<Void> mockedCallback = mock(Callback.class);
                subject.handleGoogleSignInResult(testDummyIntent, mockedCallback);

                final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                verify(mockedCallback).onResult(callbackResultCaptor.capture());
                final Result<Void> callbackResult = callbackResultCaptor.getValue();
                assertTrue("callback should have unsuccessful result", callbackResult.isError());
                assertThat(callbackResult.getError(), CoreMatchers.instanceOf(CommonException.class));
                final CommonException exception = (CommonException) callbackResult.getError();
                assertThat(exception.getMessage(), containsString("ApiException"));
                // should not interact with activity sources service
                verifyNoInteractions(mockedActivitySourcesService);
            }

            @Test
            public void handleGoogleSignInResult_whenNotAllRequiredPermissionsWereGranted_bringsErrorResultToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    context = ApplicationProvider.getApplicationContext();
                    subject = new GoogleFitActivitySource(false,
                        null,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder);

                    final Intent testIntent = new Intent();
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ)).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getSignedInAccountFromIntent(testIntent)).thenReturn(Tasks.forResult(mockedGoogleSignInAccount));

                    subject.handleGoogleSignInResult(testIntent, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), CoreMatchers.instanceOf(FitnessPermissionsNotGrantedException.class));
                    final FitnessPermissionsNotGrantedException exception = (FitnessPermissionsNotGrantedException) callbackResult.getError();
                    assertEquals("error should have the message",
                        "Not all required GoogleFit permissions were granted",
                        exception.getMessage());
                    // should not interact with activity sources service
                    verifyNoInteractions(mockedActivitySourcesService);
                }
            }

            @Test
            public void handleGoogleSignInResult_whenOfflineAccessRequiredButServerAuthCodeIsNull_bringsErrorResultToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    context = ApplicationProvider.getApplicationContext();
                    subject = new GoogleFitActivitySource(true,
                        DUMMY_SERVER_CLIENT_ID,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder);

                    final Intent testIntent = new Intent();
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ),
                            new Scope(Scopes.PROFILE),
                            new Scope(Scopes.OPEN_ID)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    when(mockedGoogleSignInAccount.getServerAuthCode()).thenReturn(null);

                    mockGoogleSignIn.when(() -> GoogleSignIn.getSignedInAccountFromIntent(testIntent)).thenReturn(Tasks.forResult(mockedGoogleSignInAccount));

                    subject.handleGoogleSignInResult(testIntent, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), CoreMatchers.instanceOf(CommonException.class));
                    final CommonException exception = (CommonException) callbackResult.getError();
                    assertEquals("error should have the message",
                        "No server auth code for the requested offline access",
                        exception.getMessage());
                    // should not interact with activity sources service
                    verifyNoInteractions(mockedActivitySourcesService);
                }
            }

            @Test
            public void handleGoogleSignInResult_whenOfflineAccessNotRequiredAndApiCallReturnsConnectedStatus_bringsResultToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    context = ApplicationProvider.getApplicationContext();
                    subject = new GoogleFitActivitySource(false,
                        DUMMY_SERVER_CLIENT_ID,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder);

                    final Intent testIntent = new Intent();
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    when(mockedGoogleSignInAccount.getServerAuthCode()).thenReturn(null);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getSignedInAccountFromIntent(testIntent)).thenReturn(Tasks.forResult(mockedGoogleSignInAccount));

                    final ApiCall<ConnectionResult> mockedApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                        final TrackerConnection gfTrackerConnection = new TrackerConnection("gf_c_id",
                            ActivitySource.TrackerValue.GOOGLE_FIT.getValue(),
                            Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                            null);
                        ConnectionResult connectedResult = new ConnectionResult.Connected(gfTrackerConnection);
                        callback.onResult(null, ApiCallResult.value(connectedResult));
                        return null;
                    }).when(mockedApiCall).enqueue(any());
                    when(mockedActivitySourcesService.connect("googlefit", new HashMap<>())).thenReturn(mockedApiCall);

                    subject.handleGoogleSignInResult(testIntent, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                    // should ask the activity sources service to connect to google fit
                    verify(mockedActivitySourcesService).connect("googlefit", new HashMap<>());
                }
            }

            @Test
            public void handleGoogleSignInResult_whenOfflineAccessRequiredAndApiCallReturnsConnectedStatus_bringsResultToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    context = ApplicationProvider.getApplicationContext();
                    subject = new GoogleFitActivitySource(true,
                        DUMMY_SERVER_CLIENT_ID,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder);

                    final Intent testIntent = new Intent();
                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ),
                            new Scope(Scopes.FITNESS_LOCATION_READ),
                            new Scope(Scopes.FITNESS_BODY_READ),
                            new Scope(Scopes.PROFILE),
                            new Scope(Scopes.OPEN_ID)
                        ),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    final String expectedAuthCode = "1234-AUTH-CODE";
                    when(mockedGoogleSignInAccount.getServerAuthCode()).thenReturn(expectedAuthCode);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getSignedInAccountFromIntent(testIntent)).thenReturn(Tasks.forResult(mockedGoogleSignInAccount));

                    final ApiCall<ConnectionResult> mockedApiCall = mock(ApiCall.class);
                    doAnswer(invocation -> {
                        final ApiCallCallback<ConnectionResult> callback = invocation.getArgument(0, ApiCallCallback.class);
                        final TrackerConnection gfTrackerConnection = new TrackerConnection("gf_c_id",
                            ActivitySource.TrackerValue.GOOGLE_FIT.getValue(),
                            Date.from(Instant.parse("2020-09-10T10:05:00Z")),
                            null);
                        ConnectionResult connectedResult = new ConnectionResult.Connected(gfTrackerConnection);
                        callback.onResult(null, ApiCallResult.value(connectedResult));
                        return null;
                    }).when(mockedApiCall).enqueue(any());
                    final Map<String, String> expectedConnectQueryParams = new HashMap<>();
                    expectedConnectQueryParams.put("code", expectedAuthCode);
                    when(mockedActivitySourcesService.connect("googlefit", expectedConnectQueryParams)).thenReturn(mockedApiCall);

                    subject.handleGoogleSignInResult(testIntent, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                    // should ask the activity sources service to connect to google fit
                    verify(mockedActivitySourcesService).connect("googlefit", expectedConnectQueryParams);
                }
            }
        }

        public static class SyncIntradayMetricsTests extends GivenRobolectricContext {
            GoogleFitActivitySource subject;
            ActivitySourcesService mockedActivitySourcesService;
            GoogleFitDataManagerBuilder mockedGfDataManagerBuilder;
            Context context;

            @Before
            public void beforeTest() {
                context = ApplicationProvider.getApplicationContext();
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
            }

            @Test
            public void syncIntradayMetrics_whenNoFitnessPermissions_bringsErrorToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(Collections.emptySet());

                    final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder()
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .include(GFIntradaySyncOptions.METRICS_TYPE.CALORIES)
                        .include(GFIntradaySyncOptions.METRICS_TYPE.HEART_RATE)
                        .include(GFIntradaySyncOptions.METRICS_TYPE.STEPS)
                        .build();
                    subject.syncIntradayMetrics(options, mockedCallback);

                    ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), instanceOf(FitnessPermissionsNotGrantedException.class));
                    // should not even interact with GoogleFitDataManager
                    verifyNoInteractions(mockedGfDataManagerBuilder);
                }
            }
        }
    }
}
