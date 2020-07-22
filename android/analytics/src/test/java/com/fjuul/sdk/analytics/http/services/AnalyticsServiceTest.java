package com.fjuul.sdk.analytics.http.services;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fjuul.sdk.analytics.entities.DailyStats;
import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.errors.ApiErrors;
import com.fjuul.sdk.http.TestApiClient;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.ApiCallResult;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AnalyticsServiceTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";
    static final String USER_TOKEN = "USER_TOKEN";
    static final String USER_SECRET = "USER_TOKEN";

    AnalyticsService analyticsService;
    MockWebServer mockWebServer;
    SigningKeychain testKeychain;
    TestApiClient.Builder clientBuilder;
    ISigningService userSigningService;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        clientBuilder = new TestApiClient.Builder(mockWebServer);
        clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        testKeychain = new SigningKeychain(new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime()));
        userSigningService = new UserSigningService(clientBuilder.build());
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void getDailyStatsTest() throws IOException {
        clientBuilder.setSigningKeychain(testKeychain);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse =
            new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Type", "application/json")
                .setBody("{\n" + "" + "\"date\": \"2020-03-10\",\n" + "\"activeKcal\": 300.23,\n"
                    + "\"totalKcal\": 502.10,\n" + "\"steps\": 4621,\n"
                    + "\"lowest\": { \"seconds\": 2400, \"metMinutes\": 5.6 },\n"
                    + "\"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                    + "\"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                    + "\"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats dailyStats = result.getValue();

        assertEquals("2020-03-10", dailyStats.getDate());
        assertEquals(300.23, dailyStats.getActiveKcal(), 0.0001);
        assertEquals(502.10, dailyStats.getTotalKcal(), 0.0001);
        assertEquals(4621, dailyStats.getSteps());
        assertEquals(5.6, dailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(2400, dailyStats.getLowest().getSeconds(), 0.0001);
        assertEquals(20, dailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, dailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(10, dailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, dailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(15, dailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, dailyStats.getHigh().getSeconds(), 0.0001);
    }

    @Test
    public void getDailyStatsRangeTest() throws IOException {
        clientBuilder.setSigningKeychain(testKeychain);
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
            .setHeader("Content-Type", "application/json")
            .setBody("[ \n" + "{\n" + "\"date\": \"2020-03-10\",\n" + "\"activeKcal\": 300,\n" + "\"totalKcal\": 500,\n"
                + "\"steps\": 4621,\n" + "\"lowest\": { \"seconds\": 2400, \"metMinutes\": 5 },\n"
                + "\"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                + "\"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                + "\"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n" + "}, \n" + "{\n"
                + "\"date\": \"2020-03-11\",\n" + "\"activeKcal\": 321,\n" + "\"totalKcal\": 550.55,\n"
                + "\"steps\": 1845,\n" + "\"lowest\": { \"seconds\": 300, \"metMinutes\": 1 },\n"
                + "\"low\": { \"seconds\": 100, \"metMinutes\": 2.1 },\n"
                + "\"moderate\": { \"seconds\": 120, \"metMinutes\": 2.3 },\n"
                + "\"high\": { \"seconds\": 30, \"metMinutes\": 3.4 }\n" + " " + "} \n" + "]");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats[]> result =
            analyticsService.getDailyStats("2020-03-10", "2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats[] dailyStatsRange = result.getValue();
        DailyStats firstDailyStats = dailyStatsRange[0];
        assertEquals("2020-03-10", firstDailyStats.getDate());
        assertEquals(300, firstDailyStats.getActiveKcal(), 0.0001);
        assertEquals(500, firstDailyStats.getTotalKcal(), 0.0001);
        assertEquals(4621, firstDailyStats.getSteps());
        assertEquals(5, firstDailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(2400, firstDailyStats.getLowest().getSeconds(), 0.0001);
        assertEquals(20, firstDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, firstDailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(10, firstDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, firstDailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(15, firstDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, firstDailyStats.getHigh().getSeconds(), 0.0001);

        DailyStats secondDailyStats = dailyStatsRange[1];
        assertEquals("2020-03-11", secondDailyStats.getDate());
        assertEquals(321, secondDailyStats.getActiveKcal(), 0.0001);
        assertEquals(550.55, secondDailyStats.getTotalKcal(), 0.0001);
        assertEquals(1845, secondDailyStats.getSteps());
        assertEquals(1, secondDailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(300, secondDailyStats.getLowest().getSeconds(), 0.0001);
        assertEquals(2.1, secondDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(100, secondDailyStats.getLow().getSeconds(), 0.0001);
        assertEquals(2.3, secondDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(120, secondDailyStats.getModerate().getSeconds(), 0.0001);
        assertEquals(3.4, secondDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(30, secondDailyStats.getHigh().getSeconds(), 0.0001);
    }

    @Test
    public void getDailyStats_EmptyKeychainWithUnauthorizedError_ReturnsErrorResult() throws IOException {
        clientBuilder.setSigningKeychain(new SigningKeychain());
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(ApiErrors.UnauthorizedError.class));
        ApiErrors.UnauthorizedError authError = (ApiErrors.UnauthorizedError) error;
        assertEquals("has wrong_credentials error code", ApiErrors.UnauthorizedError.ErrorCode.wrong_credentials,
            authError.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authError.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedError_ReturnsErrorResult() throws IOException {
        clientBuilder.setSigningKeychain(new SigningKeychain());
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(ApiErrors.UnauthorizedError.class));
        ApiErrors.UnauthorizedError authError = (ApiErrors.UnauthorizedError) error;
        assertNull("has wrong_credentials error code", authError.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authError.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedErrorWithCode_ReturnsErrorResult() throws IOException {
        clientBuilder.setSigningKeychain(new SigningKeychain());
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "wrong_credentials")
            .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(ApiErrors.UnauthorizedError.class));
        ApiErrors.UnauthorizedError authError = (ApiErrors.UnauthorizedError) error;
        assertEquals("has wrong_credentials error code", ApiErrors.UnauthorizedError.ErrorCode.wrong_credentials,
            authError.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized request", authError.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithClockSkewError_ReturnsErrorResult() throws IOException {
        clientBuilder.setSigningKeychain(new SigningKeychain());
        analyticsService = new AnalyticsService(clientBuilder.build());
        MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .setHeader("Content-Type", "application/json")
            .setHeader("x-authentication-error", "clock_skew")
            .setBody("{\n" + "    \"message\": \"Unauthorized: clock skew of 301s was greater than 300s\"" + "}");
        mockWebServer.enqueue(mockResponse);

        ApiCallResult<DailyStats> result = analyticsService.getDailyStats("2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(ApiErrors.UnauthorizedError.class));
        ApiErrors.UnauthorizedError authError = (ApiErrors.UnauthorizedError) error;
        assertEquals("has wrong_credentials error code", ApiErrors.UnauthorizedError.ErrorCode.clock_skew,
            authError.getErrorCode());
        assertEquals("has error message from response body", "Unauthorized: clock skew of 301s was greater than 300s",
            authError.getMessage());
    }
}
