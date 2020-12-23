package com.fjuul.sdk.activitysources.entities;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.fjuul.sdk.activitysources.entities.internal.GoogleFitDataManager;
import com.fjuul.sdk.activitysources.entities.internal.GoogleFitDataManagerBuilder;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.ActivityRecognitionPermissionNotGrantedException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.CommonException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.FitnessPermissionsNotGrantedException;
import com.fjuul.sdk.activitysources.exceptions.GoogleFitActivitySourceExceptions.MaxTriesCountExceededException;
import com.fjuul.sdk.activitysources.http.services.ActivitySourcesService;
import com.fjuul.sdk.core.entities.Callback;
import com.fjuul.sdk.core.entities.Result;
import com.fjuul.sdk.core.http.utils.ApiCall;
import com.fjuul.sdk.core.http.utils.ApiCallCallback;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.Tasks;

import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.util.concurrent.InlineExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class GoogleFitActivitySourceTest {
    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext { }

    static final String DUMMY_SERVER_CLIENT_ID = "google_server_client_id";
    static ExecutorService testInlineExecutor = new InlineExecutorService();

    @AfterClass
    public static void afterTests() {
        testInlineExecutor.shutdown();
    }

    @RunWith(Enclosed.class)
    public static class InstanceMethods {
        public static class AreFitnessPermissionsGrantedTests extends GivenRobolectricContext {
            GoogleFitActivitySource subject;
            ActivitySourcesService mockedActivitySourcesService;
            Set<FitnessMetricsType> collectableFitnessMetrics;
            Context context;
            GoogleFitDataManagerBuilder mockedGfDataManagerBuilder;

            @Before
            public void beforeTest() {
                collectableFitnessMetrics = Stream.of(
                    FitnessMetricsType.INTRADAY_CALORIES,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    FitnessMetricsType.INTRADAY_STEPS,
                    FitnessMetricsType.WORKOUTS
                ).collect(Collectors.toSet());
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
            }

            @Test
            public void areFitnessPermissionsGranted_whenRequireOfflineAccessAndPermissionsWereGranted_returnsTrue() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(true,
                    DUMMY_SERVER_CLIENT_ID,
                    collectableFitnessMetrics,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder,
                    testInlineExecutor);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(Fitness.SCOPE_ACTIVITY_READ, Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_BODY_READ, new Scope(Scopes.PROFILE)),
                        FitnessOptions.builder().addDataType(DataType.TYPE_HEART_RATE_BPM).build().getImpliedScopes().stream()
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);
                    assertTrue("should return true", subject.areFitnessPermissionsGranted());
                }
            }

            @Test
            public void areFitnessPermissionsGranted_whenRequireOfflineAccessAndNotAllPermissionsWereGranted_returnsFalse() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(true,
                    DUMMY_SERVER_CLIENT_ID,
                    collectableFitnessMetrics, mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder,
                    testInlineExecutor);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    // NOTE: here the profile scope is missing that required for the access from the server
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(Fitness.SCOPE_ACTIVITY_READ, Fitness.SCOPE_LOCATION_READ),
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
                    collectableFitnessMetrics, mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder,
                    testInlineExecutor);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    // NOTE: here is not a scope with profile
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(Fitness.SCOPE_ACTIVITY_READ, Fitness.SCOPE_LOCATION_READ),
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
                    collectableFitnessMetrics, mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder,
                    testInlineExecutor);

                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    // NOTE: here the location_read scope is missing that used for syncing sessions
                    Set<Scope> grantedScopes = Stream.concat(
                        Stream.of(Fitness.SCOPE_ACTIVITY_READ),
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
            Set<FitnessMetricsType> collectableFitnessMetrics;

            @Before
            public void beforeTest() {
                collectableFitnessMetrics = Stream.of(
                    FitnessMetricsType.INTRADAY_CALORIES,
                    FitnessMetricsType.INTRADAY_HEART_RATE,
                    FitnessMetricsType.INTRADAY_STEPS,
                    FitnessMetricsType.WORKOUTS
                ).collect(Collectors.toSet());
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
            }

            @Test
            public void handleGoogleSignInResult_whenWrongIntentComes_bringsApiExceptionResultToCallback() {
                context = ApplicationProvider.getApplicationContext();
                subject = new GoogleFitActivitySource(false,
                    null,
                    collectableFitnessMetrics,
                    mockedActivitySourcesService,
                    context,
                    mockedGfDataManagerBuilder,
                    testInlineExecutor);

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
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

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
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

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
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

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
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

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
            Set<FitnessMetricsType> collectableFitnessMetrics;
            Clock fixedTestClock = Clock.fixed(Instant.parse("2020-10-05T15:03:01.000Z"), ZoneId.of("UTC"));

            @Before
            public void beforeTest() {
                context = ApplicationProvider.getApplicationContext();
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
                collectableFitnessMetrics = Stream.of(
                    FitnessMetricsType.INTRADAY_CALORIES
                ).collect(Collectors.toSet());
            }

            @Test
            public void syncIntradayMetrics_whenNoNeededFitnessPermissions_bringsErrorToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(Collections.emptySet());
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .include(FitnessMetricsType.INTRADAY_CALORIES)
                        .build();

                    subject.syncIntradayMetrics(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), instanceOf(FitnessPermissionsNotGrantedException.class));
                    // should not even interact with GoogleFitDataManager
                    verifyNoInteractions(mockedGfDataManagerBuilder);
                }
            }

            @Test
            public void syncIntradayMetrics_whenGoogleFitReturnsError_bringsErrorToCallback() throws InterruptedException {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                        .collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .include(FitnessMetricsType.INTRADAY_CALORIES)
                        .build();
                    final GoogleFitDataManager mockedGfDataManager = mock(GoogleFitDataManager.class);
                    final MaxTriesCountExceededException gfException = new MaxTriesCountExceededException("Possible tries count (3) exceeded");
                    when(mockedGfDataManager.syncIntradayMetrics(options)).thenReturn(Tasks.forException(gfException));
                    when(mockedGfDataManagerBuilder.build(mockedGoogleSignInAccount)).thenReturn(mockedGfDataManager);

                    subject.syncIntradayMetrics(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertEquals("callback result should have the gf exception",
                        gfException,
                        callbackResult.getError());
                    // should ask GoogleFitDataManager to create an instance of GoogleFitDataManager
                    verify(mockedGfDataManagerBuilder).build(mockedGoogleSignInAccount);
                    // should ask GoogleFitDataManager to sync intraday data
                    verify(mockedGfDataManager).syncIntradayMetrics(options);
                }
            }

            @Test
            public void syncIntradayMetrics_whenGoogleFitReturnsSuccessfulTask_bringsResultToCallback() throws InterruptedException {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                        .collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFIntradaySyncOptions options = new GFIntradaySyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .include(FitnessMetricsType.INTRADAY_CALORIES)
                        .build();
                    final GoogleFitDataManager mockedGfDataManager = mock(GoogleFitDataManager.class);
                    when(mockedGfDataManager.syncIntradayMetrics(options)).thenReturn(Tasks.forResult(null));
                    when(mockedGfDataManagerBuilder.build(mockedGoogleSignInAccount)).thenReturn(mockedGfDataManager);

                    subject.syncIntradayMetrics(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                    // should ask GoogleFitDataManager to create an instance of GoogleFitDataManager
                    verify(mockedGfDataManagerBuilder).build(mockedGoogleSignInAccount);
                    // should ask GoogleFitDataManager to sync intraday data
                    verify(mockedGfDataManager).syncIntradayMetrics(options);
                }
            }
        }

        public static class SyncSessionsTests extends GivenRobolectricContext {
            GoogleFitActivitySource subject;
            ActivitySourcesService mockedActivitySourcesService;
            GoogleFitDataManagerBuilder mockedGfDataManagerBuilder;
            Context context;
            Set<FitnessMetricsType> collectableFitnessMetrics;
            Clock fixedTestClock = Clock.fixed(Instant.parse("2020-10-05T15:03:01.000Z"), ZoneId.of("UTC"));

            @Before
            public void beforeTest() {
                context = ApplicationProvider.getApplicationContext();
                mockedActivitySourcesService = mock(ActivitySourcesService.class);
                mockedGfDataManagerBuilder = mock(GoogleFitDataManagerBuilder.class);
                collectableFitnessMetrics = Stream.of(
                    FitnessMetricsType.WORKOUTS
                ).collect(Collectors.toSet());
            }

            @Test
            public void syncSessions_whenNoNeededFitnessPermissions_bringsErrorToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics, mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(Collections.emptySet());
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFSessionSyncOptions options = new GFSessionSyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .setMinimumSessionDuration(Duration.ofMinutes(3))
                        .build();

                    subject.syncSessions(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), instanceOf(FitnessPermissionsNotGrantedException.class));
                    // should not even interact with GoogleFitDataManager
                    verifyNoInteractions(mockedGfDataManagerBuilder);
                }
            }

            @Test
            public void syncSessions_whenNoActivityRecognitionPermission_bringsErrorToCallback() {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFSessionSyncOptions options = new GFSessionSyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .setMinimumSessionDuration(Duration.ofMinutes(3))
                        .build();

                    subject.syncSessions(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertThat(callbackResult.getError(), instanceOf(ActivityRecognitionPermissionNotGrantedException.class));
                    ActivityRecognitionPermissionNotGrantedException exception = (ActivityRecognitionPermissionNotGrantedException) callbackResult.getError();
                    assertEquals("error result should have message",
                        "ACTIVITY_RECOGNITION permission not granted",
                        exception.getMessage());
                    // should not even interact with GoogleFitDataManager
                    verifyNoInteractions(mockedGfDataManagerBuilder);
                }
            }

            @Test
            public void syncSessions_whenGoogleFitReturnsError_bringsErrorToCallback() throws InterruptedException {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
                    shadowApplication.grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION);
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFSessionSyncOptions options = new GFSessionSyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .setMinimumSessionDuration(Duration.ofMinutes(3))
                        .build();
                    final GoogleFitDataManager mockedGfDataManager = mock(GoogleFitDataManager.class);
                    final MaxTriesCountExceededException gfException = new MaxTriesCountExceededException("Possible tries count (3) exceeded");
                    when(mockedGfDataManager.syncSessions(options)).thenReturn(Tasks.forException(gfException));
                    when(mockedGfDataManagerBuilder.build(mockedGoogleSignInAccount)).thenReturn(mockedGfDataManager);

                    subject.syncSessions(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertTrue("callback should have unsuccessful result", callbackResult.isError());
                    assertEquals("callback result should have the gf exception",
                        gfException,
                        callbackResult.getError());
                    // should ask GoogleFitDataManager to create an instance of GoogleFitDataManager
                    verify(mockedGfDataManagerBuilder).build(mockedGoogleSignInAccount);
                    // should ask GoogleFitDataManager to sync intraday data
                    verify(mockedGfDataManager).syncSessions(options);
                }
            }

            @Test
            public void syncSessions_whenGoogleFitReturnsSuccessfulTask_bringsResultToCallback() throws InterruptedException {
                try (MockedStatic<GoogleSignIn> mockGoogleSignIn = mockStatic(GoogleSignIn.class)) {
                    ShadowApplication shadowApplication = Shadows.shadowOf((Application) context);
                    shadowApplication.grantPermissions(Manifest.permission.ACTIVITY_RECOGNITION);
                    subject = new GoogleFitActivitySource(false,
                        null,
                        collectableFitnessMetrics,
                        mockedActivitySourcesService,
                        context,
                        mockedGfDataManagerBuilder,
                        testInlineExecutor);

                    final Callback<Void> mockedCallback = mock(Callback.class);
                    final GoogleSignInAccount mockedGoogleSignInAccount = mock(GoogleSignInAccount.class);
                    final Set<Scope> grantedScopes = Stream.of(Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toSet());
                    when(mockedGoogleSignInAccount.getGrantedScopes()).thenReturn(grantedScopes);
                    mockGoogleSignIn.when(() -> GoogleSignIn.getLastSignedInAccount(context)).thenReturn(mockedGoogleSignInAccount);

                    final GFSessionSyncOptions options = new GFSessionSyncOptions.Builder(fixedTestClock)
                        .setDateRange(LocalDate.parse("2020-10-01"), LocalDate.parse("2020-10-03"))
                        .setMinimumSessionDuration(Duration.ofMinutes(3))
                        .build();
                    final GoogleFitDataManager mockedGfDataManager = mock(GoogleFitDataManager.class);
                    when(mockedGfDataManager.syncSessions(options)).thenReturn(Tasks.forResult(null));
                    when(mockedGfDataManagerBuilder.build(mockedGoogleSignInAccount)).thenReturn(mockedGfDataManager);

                    subject.syncSessions(options, mockedCallback);

                    final ArgumentCaptor<Result<Void>> callbackResultCaptor = ArgumentCaptor.forClass(Result.class);
                    verify(mockedCallback).onResult(callbackResultCaptor.capture());
                    final Result<Void> callbackResult = callbackResultCaptor.getValue();
                    assertFalse("callback should have successful result", callbackResult.isError());
                    // should ask GoogleFitDataManager to create an instance of GoogleFitDataManager
                    verify(mockedGfDataManagerBuilder).build(mockedGoogleSignInAccount);
                    // should ask GoogleFitDataManager to sync intraday data
                    verify(mockedGfDataManager).syncSessions(options);
                }
            }
        }
    }

    @RunWith(Enclosed.class)
    public static class StaticMethods {
        public static class BuildGoogleSignInOptionsTests extends GivenRobolectricContext {
            @Test
            public void buildGoogleSignInOptions_whenNoOfflineAccessAndFitnessMetricsHasOnlyCalories_returnsOptionsWithScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(false,
                    null,
                    Stream.of(FitnessMetricsType.INTRADAY_CALORIES).collect(Collectors.toSet()));
                assertEquals("should have only fitness.activity.read scope",
                    Stream.of(Fitness.SCOPE_ACTIVITY_READ).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenNoOfflineAccessAndFitnessMetricsHasOnlySteps_returnsOptionsWithScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(false,
                    null,
                    Stream.of(FitnessMetricsType.INTRADAY_STEPS).collect(Collectors.toSet()));
                assertEquals("should have only fitness.activity.read scope",
                    Stream.of(Fitness.SCOPE_ACTIVITY_READ).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenNoOfflineAccessAndFitnessMetricsAreCaloriesAndSteps_returnsOptionsWithScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(false,
                    null,
                    Stream.of(FitnessMetricsType.INTRADAY_STEPS, FitnessMetricsType.INTRADAY_CALORIES).collect(Collectors.toSet()));
                assertEquals("should have only fitness.activity.read scope",
                    Stream.of(Fitness.SCOPE_ACTIVITY_READ).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenNoOfflineAccessAndFitnessMetricsHasOnlyHeartRate_returnsOptionsWithScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(false,
                    null,
                    Stream.of(FitnessMetricsType.INTRADAY_HEART_RATE).collect(Collectors.toSet()));
                assertEquals("should have only fitness.heart_rate.read scope",
                    Stream.of(new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenNoOfflineAccessAndFitnessMetricsHasOnlyWorkouts_returnsOptionsWithScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(false,
                    null,
                    Stream.of(FitnessMetricsType.WORKOUTS).collect(Collectors.toSet()));
                assertEquals("should have several scopes",
                    Stream.of(Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenOfflineAccessAndFitnessMetricsHasOnlyCalories_returnsOptionsWithAllScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(true,
                    DUMMY_SERVER_CLIENT_ID,
                    Stream.of(FitnessMetricsType.INTRADAY_CALORIES).collect(Collectors.toSet()));
                // NOTE: currently implementation of the server will request all data to perform the sync,
                // that's why we request all needed scopes.
                assertEquals("should include all scopes anyways",
                    Stream.of(new Scope(Scopes.PROFILE),
                        Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_BODY_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toList()),
                    result.getScopes());
            }

            @Test
            public void buildGoogleSignInOptions_whenFullIntegration_returnsOptionsWithAllScopes() {
                final GoogleSignInOptions result = GoogleFitActivitySource.buildGoogleSignInOptions(true,
                    DUMMY_SERVER_CLIENT_ID,
                    // pass every possible fitness metrics
                    Stream.of(FitnessMetricsType.class.getEnumConstants()).collect(Collectors.toSet()));
                assertEquals("should have all expected scopes",
                    Stream.of(new Scope(Scopes.PROFILE),
                        Fitness.SCOPE_LOCATION_READ, Fitness.SCOPE_BODY_READ, Fitness.SCOPE_ACTIVITY_READ,
                        new Scope("https://www.googleapis.com/auth/fitness.heart_rate.read")
                    ).collect(Collectors.toList()),
                    result.getScopes());
                assertTrue("should request the server auth code", result.isServerAuthCodeRequested());
            }
        }
    }
}
