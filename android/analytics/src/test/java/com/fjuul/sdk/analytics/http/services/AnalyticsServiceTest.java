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
import com.fjuul.sdk.http.TestHttpClientBuilder;
import com.fjuul.sdk.http.errors.HttpErrors;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.Result;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class AnalyticsServiceTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";
    static final String USER_TOKEN = "USER_TOKEN";
    static final String USER_SECRET = "USER_TOKEN";

    AnalyticsService analyticsService;
    MockWebServer mockWebServer;

    @Before
    public void setup() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        TestHttpClientBuilder clientBuilder = new TestHttpClientBuilder(mockWebServer);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        SigningKeychain testKeychain =
                new SigningKeychain(new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime()));
        ISigningService userSigningService =
                new UserSigningService(clientBuilder, new UserCredentials(USER_TOKEN, USER_SECRET));
        analyticsService = new AnalyticsService(clientBuilder, testKeychain, userSigningService);
    }

    @After
    public void teardown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void getDailyStatsTest() throws IOException {
        MockResponse mockResponse =
                new MockResponse()
                        .setResponseCode(HttpURLConnection.HTTP_OK)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                "{\n"
                                        + "    \"date\": \"2020-03-10\",\n"
                                        + "    \"activeKcal\": 300.23,\n"
                                        + "    \"totalKcal\": 502.10,\n"
                                        + "    \"steps\": 4621,\n"
                                        + "    \"lowest\": { \"seconds\": 2400, \"metMinutes\": 5.6 },\n"
                                        + "    \"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                                        + "    \"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                                        + "    \"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n"
                                        + "}");
        mockWebServer.enqueue(mockResponse);

        Result<DailyStats, HttpErrors.CommonError> result =
                analyticsService.getDailyStats(USER_TOKEN, "2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats dailyStats = result.getValue();

        assertEquals("2020-03-10", dailyStats.getDate());
        assertEquals(300.23, dailyStats.getActiveKcal(), 0.0001);
        assertEquals(502.10, dailyStats.getTotalKcal(), 0.0001);
        assertEquals(4621, dailyStats.getSteps());
        assertEquals(5.6, dailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(2400, dailyStats.getLowest().getSeconds());
        assertEquals(20, dailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, dailyStats.getLow().getSeconds());
        assertEquals(10, dailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, dailyStats.getModerate().getSeconds());
        assertEquals(15, dailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, dailyStats.getHigh().getSeconds());
    }

    @Test
    public void getDailyStatsRangeTest() throws IOException {
        MockResponse mockResponse =
                new MockResponse()
                        .setResponseCode(HttpURLConnection.HTTP_OK)
                        .setHeader("Content-Type", "application/json")
                        .setBody(
                                "[\n"
                                        + "    {\n"
                                        + "        \"date\": \"2020-03-10\",\n"
                                        + "        \"activeKcal\": 300,\n"
                                        + "        \"totalKcal\": 500,\n"
                                        + "        \"steps\": 4621,\n"
                                        + "        \"lowest\": { \"seconds\": 2400, \"metMinutes\": 5 },\n"
                                        + "        \"low\": { \"seconds\": 1800, \"metMinutes\": 20 },\n"
                                        + "        \"moderate\": { \"seconds\": 1200, \"metMinutes\": 10 },\n"
                                        + "        \"high\": { \"seconds\": 180, \"metMinutes\": 15 }\n"
                                        + "    },\n"
                                        + "    {\n"
                                        + "        \"date\": \"2020-03-11\",\n"
                                        + "        \"activeKcal\": 321,\n"
                                        + "        \"totalKcal\": 550.55,\n"
                                        + "        \"steps\": 1845,\n"
                                        + "        \"lowest\": { \"seconds\": 300, \"metMinutes\": 1 },\n"
                                        + "        \"low\": { \"seconds\": 100, \"metMinutes\": 2.1 },\n"
                                        + "        \"moderate\": { \"seconds\": 120, \"metMinutes\": 2.3 },\n"
                                        + "        \"high\": { \"seconds\": 30, \"metMinutes\": 3.4 }\n"
                                        + "    }\n"
                                        + "]");
        mockWebServer.enqueue(mockResponse);

        Result<DailyStats[], HttpErrors.CommonError> result =
                analyticsService.getDailyStats(USER_TOKEN, "2020-03-10", "2020-03-10").execute();

        assertFalse("success result", result.isError());
        DailyStats[] dailyStatsRange = result.getValue();
        DailyStats firstDailyStats = dailyStatsRange[0];
        assertEquals("2020-03-10", firstDailyStats.getDate());
        assertEquals(300, firstDailyStats.getActiveKcal(), 0.0001);
        assertEquals(500, firstDailyStats.getTotalKcal(), 0.0001);
        assertEquals(4621, firstDailyStats.getSteps());
        assertEquals(5, firstDailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(2400, firstDailyStats.getLowest().getSeconds());
        assertEquals(20, firstDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(1800, firstDailyStats.getLow().getSeconds());
        assertEquals(10, firstDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(1200, firstDailyStats.getModerate().getSeconds());
        assertEquals(15, firstDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(180, firstDailyStats.getHigh().getSeconds());

        DailyStats secondDailyStats = dailyStatsRange[1];
        assertEquals("2020-03-11", secondDailyStats.getDate());
        assertEquals(321, secondDailyStats.getActiveKcal(), 0.0001);
        assertEquals(550.55, secondDailyStats.getTotalKcal(), 0.0001);
        assertEquals(1845, secondDailyStats.getSteps());
        assertEquals(1, secondDailyStats.getLowest().getMetMinutes(), 0.0001);
        assertEquals(300, secondDailyStats.getLowest().getSeconds());
        assertEquals(2.1, secondDailyStats.getLow().getMetMinutes(), 0.0001);
        assertEquals(100, secondDailyStats.getLow().getSeconds());
        assertEquals(2.3, secondDailyStats.getModerate().getMetMinutes(), 0.0001);
        assertEquals(120, secondDailyStats.getModerate().getSeconds());
        assertEquals(3.4, secondDailyStats.getHigh().getMetMinutes(), 0.0001);
        assertEquals(30, secondDailyStats.getHigh().getSeconds());
    }

    @Test
    public void getDailyStats_ResponseWithUnauthorizedError_ReturnsErrorResult()
            throws IOException {
        MockResponse mockResponse =
                new MockResponse()
                        .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                        .setHeader("Content-Type", "application/json")
                        .setHeader("x-authentication-error", "wrong_credentials")
                        .setBody("{\n" + "    \"message\": \"Unauthorized request\"" + "}");
        mockWebServer.enqueue(mockResponse);

        Result<DailyStats, HttpErrors.CommonError> result =
                analyticsService.getDailyStats(USER_TOKEN, "2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(HttpErrors.UnauthorizedError.class));
        HttpErrors.UnauthorizedError authError = (HttpErrors.UnauthorizedError) error;
        assertEquals(
                "has wrong_credentials error code",
                HttpErrors.UnauthorizedError.ErrorCode.wrong_credentials,
                authError.getErrorCode());
        assertEquals(
                "has error message from response body",
                "Unauthorized request",
                authError.getMessage());
    }

    @Test
    public void getDailyStats_ResponseWithClockSkewError_ReturnsErrorResult() throws IOException {
        MockResponse mockResponse =
                new MockResponse()
                        .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                        .setHeader("Content-Type", "application/json")
                        .setHeader("x-authentication-error", "clock_skew")
                        .setBody(
                                "{\n"
                                        + "    \"message\": \"Unauthorized: clock skew of 301s was greater than 300s\""
                                        + "}");
        mockWebServer.enqueue(mockResponse);

        Result<DailyStats, HttpErrors.CommonError> result =
                analyticsService.getDailyStats(USER_TOKEN, "2020-03-10").execute();

        assertTrue("error result", result.isError());
        Error error = result.getError();
        assertThat(result.getError(), IsInstanceOf.instanceOf(HttpErrors.UnauthorizedError.class));
        HttpErrors.UnauthorizedError authError = (HttpErrors.UnauthorizedError) error;
        assertEquals(
                "has wrong_credentials error code",
                HttpErrors.UnauthorizedError.ErrorCode.clock_skew,
                authError.getErrorCode());
        assertEquals(
                "has error message from response body",
                "Unauthorized: clock skew of 301s was greater than 300s",
                authError.getMessage());
    }
}
