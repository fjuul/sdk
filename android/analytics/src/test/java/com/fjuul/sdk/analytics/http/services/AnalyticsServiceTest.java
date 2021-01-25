package com.fjuul.sdk.analytics.http.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.core.entities.InMemoryStorage;
import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.SigningKey;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.fixtures.http.TestApiClient;
import com.fjuul.sdk.core.http.services.ISigningService;
import com.fjuul.sdk.core.http.services.UserSigningService;
import com.fjuul.sdk.core.http.utils.ApiCallResult;

import android.os.Build;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
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
        testKeystore = new Keystore(new InMemoryStorage(), USER_TOKEN);
        userSigningService = new UserSigningService(clientBuilder.build());
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void getDailyStatsTest() throws IOException {
        testKeystore.setKey(validSigningKey);
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("{\n" + "" + "\"date\": \"2020-03-10\",\n" + "\"activeKcal\": 300.23,\n" + "\"bmr\": 502.10,\n"
                + "\"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                + "\"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                + "\"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats dailyStats = result.getValue();

        assertEquals("2020-03-10", dailyStats.getDate());
        assertEquals(300.23, dailyStats.getActiveKcal(), 0.0001);
        assertEquals(502.10, dailyStats.getBmr(), 0.0001);
        assertEquals(20, dailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, dailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(10, dailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, dailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(15, dailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, dailyStats.getHigh().getSeconds(), 0.0001);
    }

    @Test
    public void getDailyStatsRangeTest() throws IOException {
        testKeystore.setKey(validSigningKey);
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("[ \n" + "{\n" + "\"date\": \"2020-03-10\",\n" + "\"activeKcal\": 300,\n" + "\"bmr\": 500,\n"
                + "\"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                + "\"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                + "\"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n" + "}, \n" + "{\n"
                + "\"date\": \"2020-03-11\",\n" + "\"activeKcal\": 321,\n" + "\"bmr\": 550.55,\n"
                + "\"low\": { \"seconds\": 100, \"metMinutes\": 2.1 },\n"
                + "\"moderate\": { \"seconds\": 120, \"metMinutes\": 2.3 },\n"
                + "\"high\": { \"seconds\": 30, \"metMinutes\": 3.4 }\n" + " " + "} \n" + "]");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats[]> result = analyticsService.getDailyStats("2020-03-10", "2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats[] dailyStatsRange = result.getValue();
        DailyStats firstDailyStats = dailyStatsRange[0];
        assertEquals("2020-03-10", firstDailyStats.getDate());
        assertEquals(300, firstDailyStats.getActiveKcal(), 0.0001);
        assertEquals(500, firstDailyStats.getBmr(), 0.0001);
        assertEquals(20, firstDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, firstDailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(10, firstDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, firstDailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(15, firstDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, firstDailyStats.getHigh().getSeconds(), 0.0001);

        DailyStats secondDailyStats = dailyStatsRange[1];
        assertEquals("2020-03-11", secondDailyStats.getDate());
        assertEquals(321, secondDailyStats.getActiveKcal(), 0.0001);
        assertEquals(550.55, secondDailyStats.getBmr(), 0.0001);
        assertEquals(2.1, secondDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(100, secondDailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(2.3, secondDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(120, secondDailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(3.4, secondDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(30, secondDailyStats.getHigh().getSeconds(), 0.0001);
    }

    @Test
    public void getDailyStats_EmptyKeystoreWithUnauthorizedException_ReturnsErrorResult() throws IOException {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.wrong_credentials,
            authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedException_ReturnsErrorResult() throws IOException {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertNull("has wrong_credentials error code", authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedExceptionWithCode_ReturnsErrorResult() throws IOException {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.wrong_credentials,
            authException.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authException.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithClockSkewException_ReturnsErrorResult() throws IOException {
        clientBuilder.setKeystore(testKeystore);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "clock_skew")
            .setBody("{\n" + "    \"message\": \"Unauthorized: clock skew of 301s was greater than 300s\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Exception exception = result.getError();
        assertThat(exception, IsInstanceOf.instanceOf(ApiExceptions.UnauthorizedException.class));
        ApiExceptions.UnauthorizedException authException = (ApiExceptions.UnauthorizedException) exception;
        assertEquals("has wrong_credentials error code",
            ApiExceptions.UnauthorizedException.ErrorCode.clock_skew,
            authException.getErrorCode());
        assertEquals("has error message from response body",
            "Unauthorized: clock skew of 301s was greater than 300s",
            authException.getMessage());
    }
}
