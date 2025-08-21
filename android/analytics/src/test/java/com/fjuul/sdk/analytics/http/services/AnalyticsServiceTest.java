package com.fjuul.sdk.analytics.http.services;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Calendar;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.analytics.entities.AggregatedDailyStats;
import com.fjuul.sdk.analytics.entities.AggregationType;
import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.core.entities.InMemoryStorage;
import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.SigningKey;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.services.ISigningService;
import com.fjuul.sdk.core.http.services.UserSigningService;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.test.http.TestApiClient;

import android.os.Build;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class AnalyticsServiceTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";
    static final String USER_TOKEN = "USER_TOKEN";
    static final String USER_SECRET = "USER_TOKEN";

    AnalyticsService analyticsService;
    MockWebServer mockWebServer;
    Keystore testKeystore;
    TestApiClient.Builder clientBuilder;
    ISigningService userSigningService;
    SigningKey validSigningKey;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        clientBuilder = new TestApiClient.Builder(mockWebServer);
        clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
        testKeystore = new Keystore(new InMemoryStorage());
        userSigningService = new UserSigningService(clientBuilder.build());
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void getDailyStatsTest() throws InterruptedException {
        testKeystore.setKey(validSigningKey);
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                "date": "2020-03-10",
                "activeKcal": 0,
                "bmr": 0,
                "steps": 8522,
                "low": { "seconds": 1800, "metMinutes": 20 },
                "moderate": { "seconds": 1200, "metMinutes": 10 },
                "high": { "seconds": 180, "metMinutes": 15 },
                "contributingSources": ["fitbit", "garmin"]
                }""");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats(LocalDate.parse("2020-03-10")).execute();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat("should transform local date to string",
            request.getPath(),
            containsString("daily-stats/USER_TOKEN/2020-03-10"));

        assertFalse("success result", result.isError());
        DailyStats dailyStats = result.getValue();

        assertNotNull(dailyStats);
        assertEquals("2020-03-10", dailyStats.getDate());
        assertEquals(8522, dailyStats.getSteps());
        assertEquals(20, dailyStats.getLow().getMetMinutes());
        assertEquals(1800, dailyStats.getLow().getSeconds());
        assertEquals(10, dailyStats.getModerate().getMetMinutes());
        assertEquals(1200, dailyStats.getModerate().getSeconds());
        assertEquals(15, dailyStats.getHigh().getMetMinutes());
        assertEquals(180, dailyStats.getHigh().getSeconds());
        assertArrayEquals(new String[] { "fitbit", "garmin" }, dailyStats.getContributingSources());
    }

    @Test
    public void getDailyStatsRangeTest() throws InterruptedException {
        testKeystore.setKey(validSigningKey);
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                [\s
                {
                "date": "2020-03-10",
                "activeKcal": 0,
                "bmr": 0,
                "steps": 8900,
                "low": { "seconds": 1800, "metMinutes": 20 },
                "moderate": { "seconds": 1200, "metMinutes": 10 },
                "high": { "seconds": 180, "metMinutes": 15 },
                "contributingSources": ["polar"]
                },\s
                {
                "date": "2020-03-11",
                "activeKcal": 0,
                "bmr": 0,
                "steps": 9010,
                "low": { "seconds": 100, "metMinutes": 2 },
                "moderate": { "seconds": 120, "metMinutes": 2 },
                "high": { "seconds": 30, "metMinutes": 3 },
                "contributingSources": ["healthconnect"]
                 }\s
                ]""");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats[]> result =
            analyticsService.getDailyStats(LocalDate.parse("2020-03-10"), LocalDate.parse("2020-03-11")).execute();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat("should transform local date to string",
            request.getPath(),
            containsString("daily-stats/USER_TOKEN?from=2020-03-10&to=2020-03-11"));

        assertFalse("success result", result.isError());
        DailyStats[] dailyStatsRange = result.getValue();
        assertNotNull(dailyStatsRange);
        DailyStats firstDailyStats = dailyStatsRange[0];
        assertEquals("2020-03-10", firstDailyStats.getDate());
        assertEquals(8900, firstDailyStats.getSteps());
        assertEquals(20, firstDailyStats.getLow().getMetMinutes());
        assertEquals(1800, firstDailyStats.getLow().getSeconds());
        assertEquals(10, firstDailyStats.getModerate().getMetMinutes());
        assertEquals(1200, firstDailyStats.getModerate().getSeconds());
        assertEquals(15, firstDailyStats.getHigh().getMetMinutes());
        assertEquals(180, firstDailyStats.getHigh().getSeconds());
        assertArrayEquals(new String[] { "polar" }, firstDailyStats.getContributingSources());

        DailyStats secondDailyStats = dailyStatsRange[1];
        assertEquals("2020-03-11", secondDailyStats.getDate());
        assertEquals(9010, secondDailyStats.getSteps());
        assertEquals(2, secondDailyStats.getLow().getMetMinutes());
        assertEquals(100, secondDailyStats.getLow().getSeconds());
        assertEquals(2, secondDailyStats.getModerate().getMetMinutes());
        assertEquals(120, secondDailyStats.getModerate().getSeconds());
        assertEquals(3, secondDailyStats.getHigh().getMetMinutes());
        assertEquals(30, secondDailyStats.getHigh().getSeconds());
        assertArrayEquals(new String[] { "healthconnect" }, secondDailyStats.getContributingSources());

    }

    @Test
    public void getAggregatedDailyStatsTest() throws InterruptedException {
        testKeystore.setKey(validSigningKey);
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {
                "activeKcal": 0,
                "bmr": 0,
                "steps": 10103,
                "low": { "seconds": 2222, "metMinutes": 32 },
                "moderate": { "seconds": 1980, "metMinutes": 44 },
                "high": { "seconds": 600, "metMinutes": 9 },
                "contributingSources": ["suunto", "withings"]
                }""");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<AggregatedDailyStats> result = analyticsService
            .getAggregatedDailyStats(LocalDate.parse("2020-03-10"), LocalDate.parse("2020-03-11"), AggregationType.sum)
            .execute();

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat("should transform local date to string",
            request.getPath(),
            containsString("daily-stats/USER_TOKEN/aggregated?from=2020-03-10&to=2020-03-11&aggregation=sum"));

        assertFalse("success result", result.isError());
        AggregatedDailyStats aggregatedStats = result.getValue();

        assertNotNull(aggregatedStats);
        assertEquals(10103, aggregatedStats.getSteps());
        assertEquals(32, aggregatedStats.getLow().getMetMinutes());
        assertEquals(2222, aggregatedStats.getLow().getSeconds());
        assertEquals(44, aggregatedStats.getModerate().getMetMinutes());
        assertEquals(1980, aggregatedStats.getModerate().getSeconds());
        assertEquals(9, aggregatedStats.getHigh().getMetMinutes());
        assertEquals(600, aggregatedStats.getHigh().getSeconds());
        assertArrayEquals(new String[] { "suunto", "withings" }, aggregatedStats.getContributingSources());
    }

    @Test
    public void getDailyStats_EmptyKeystoreWithUnauthorizedException_ReturnsErrorResult() {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats(LocalDate.parse("2020-03-10")).execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertNotNull(authException);
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.wrong_credentials,
            authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedException_ReturnsErrorResult() {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats(LocalDate.parse("2020-03-10")).execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertNotNull(authException);
        assertNull("has wrong_credentials error code", authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedExceptionWithCode_ReturnsErrorResult() {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats(LocalDate.parse("2020-03-10")).execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertNotNull(authException);
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.wrong_credentials,
            authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithClockSkewException_ReturnsErrorResult() {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "clock_skew")
            .setBody("{\n" + "    \"message\": \"Unauthorized: clock skew of 301s was greater than 300s\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats(LocalDate.parse("2020-03-10")).execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertNotNull(authException);
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.clock_skew,
            authException.getErrorCode());
        assertEquals("has error message from response body",
            "Unauthorized: clock skew of 301s was greater than 300s",
            authException.getMessage());
    }
}
