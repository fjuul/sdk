package com.fjuul.sdk.activitysources.http.services;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.activitysources.entities.ConnectionResult;
import com.fjuul.sdk.activitysources.entities.ConnectionResult.ExternalAuthenticationFlowRequired;
import com.fjuul.sdk.activitysources.entities.TrackerConnection;
import com.fjuul.sdk.activitysources.entities.internal.GFSynchronizableProfileParams;
import com.fjuul.sdk.activitysources.entities.internal.GFUploadData;
import com.fjuul.sdk.activitysources.entities.internal.googlefit.GFCalorieDataPoint;
import com.fjuul.sdk.activitysources.exceptions.ActivitySourcesApiExceptions.SourceAlreadyConnectedException;
import com.fjuul.sdk.core.entities.InMemoryStorage;
import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.SigningKey;
import com.fjuul.sdk.core.entities.UserCredentials;
import com.fjuul.sdk.core.exceptions.ApiExceptions;
import com.fjuul.sdk.core.http.utils.ApiCallResult;
import com.fjuul.sdk.test.http.TestApiClient;

import android.os.Build;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(Enclosed.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class ActivitySourcesServiceTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";
    static final String USER_TOKEN = "USER_TOKEN";
    static final String USER_SECRET = "USER_TOKEN";

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static class ConnectToTracker extends GivenRobolectricContext {
        ActivitySourcesService sourcesService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;
        Keystore testKeystore;
        SigningKey validSigningKey;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 2);
            validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
            testKeystore = new Keystore(new InMemoryStorage());
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void connect_WithHttpOkResponseCode_RespondWithExternalAuthenticationFlow()
            throws IOException, InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"url\": \"https://connect.garmin.com/oauthConfirm?oauth_token=9be0741a\" }");
            mockWebServer.enqueue(mockResponse);

            ApiCallResult<ConnectionResult> result = sourcesService.connect("garmin").execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertFalse("successful result", result.isError());
            assertThat(result.getValue(), instanceOf(ExternalAuthenticationFlowRequired.class));
            ExternalAuthenticationFlowRequired trackerAuthentication =
                (ExternalAuthenticationFlowRequired) result.getValue();
            assertEquals("should have auth url",
                "https://connect.garmin.com/oauthConfirm?oauth_token=9be0741a",
                trackerAuthentication.getUrl());
        }

        @Test
        public void connect_WithHttpCreatedResponseCode_RespondWithTrackerConnection() throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_CREATED)
                .setHeader("Content-Type", "application/json")
                .setBody("{\n" + "    \"tracker\": \"googlefit\",\n"
                    + "    \"createdAt\": \"2020-05-18T15:30:53.978Z\",\n" + "    \"endedAt\": null,\n"
                    + "    \"user\": {\n" + "        \"token\": \"7733f6b0-****-****-9684-03421262e9a1\"\n" + "    },\n"
                    + "    \"id\": \"09f6e64b-63cc-41a2-8a4a-076bbd981077\"\n" + "}");
            mockWebServer.enqueue(mockResponse);

            ApiCallResult<ConnectionResult> result = sourcesService.connect("googlefit").execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertFalse("successful result", result.isError());
            assertThat(result.getValue(), instanceOf(ConnectionResult.Connected.class));
            ConnectionResult.Connected connectionResult = (ConnectionResult.Connected) result.getValue();
            assertNotNull("should have tracker connection", connectionResult.getTrackerConnection());
            TrackerConnection trackerConnection = connectionResult.getTrackerConnection();
            assertEquals("should have tracker name", "googlefit", trackerConnection.getTracker());
            assertEquals("should have tracker id", "09f6e64b-63cc-41a2-8a4a-076bbd981077", trackerConnection.getId());
            assertEquals("should have createdAt",
                Date.from(ZonedDateTime.parse("2020-05-18T15:30:53.978Z").toInstant()),
                trackerConnection.getCreatedAt());
            assertNull("should not have endedAt", trackerConnection.getEndedAt());
        }

        @Test
        public void connect_WithHttpConflictResponseCode_RespondWithException() throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_CONFLICT)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"message\": \"tracker \\\"googlefit\\\" already connected\" }");
            mockWebServer.enqueue(mockResponse);

            ApiCallResult<ConnectionResult> result = sourcesService.connect("googlefit").execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertTrue("unsuccessful result", result.isError());
            assertThat(result.getError(), instanceOf(SourceAlreadyConnectedException.class));
            SourceAlreadyConnectedException exception = (SourceAlreadyConnectedException) result.getError();
            assertEquals("should have error message",
                "tracker \"googlefit\" already connected",
                exception.getMessage());
        }
    }

    public static class DisconnectTracker extends GivenRobolectricContext {
        ActivitySourcesService sourcesService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;
        Keystore testKeystore;
        SigningKey validSigningKey;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 2);
            validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
            testKeystore = new Keystore(new InMemoryStorage());
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void disconnect_WithHttpNoContentResponseCode_RespondWithoutException()
            throws IOException, InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_NO_CONTENT);
            mockWebServer.enqueue(mockResponse);

            TrackerConnection testConnection = new TrackerConnection("connection_id", "garmin", new Date(), new Date());

            ApiCallResult<Void> result = sourcesService.disconnect(testConnection).execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertFalse("successful result", result.isError());
        }

        @Test
        public void disconnect_WithHttpNotFoundResponseCode_RespondWithException()
            throws IOException, InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
                .setHeader("Content-Type", "application/json")
                .setBody("{ \"message\": \"tracker \\\"connection_id\\\" not found\" }");
            mockWebServer.enqueue(mockResponse);

            TrackerConnection testConnection = new TrackerConnection("connection_id", "garmin", new Date(), new Date());

            ApiCallResult<Void> result = sourcesService.disconnect(testConnection).execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertTrue("unsuccessful result", result.isError());
            ApiExceptions.CommonException exception = result.getError();
            assertEquals("should have error message", "tracker \"connection_id\" not found", exception.getMessage());
        }
    }

    public static class GetCurrentConnections extends GivenRobolectricContext {
        ActivitySourcesService sourcesService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;
        Keystore testKeystore;
        SigningKey validSigningKey;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 2);
            validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
            testKeystore = new Keystore(new InMemoryStorage());
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void getCurrentConnections_WithHttpOkResponseCode_RespondWithArrayOfConnections()
            throws IOException, InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());
            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Type", "application/json")
                .setBody("[\n" + "    {\n" + "        \"id\": \"0c98f062-055e-42fe-9c30-3a9645716d6f\",\n"
                    + "        \"tracker\": \"fitbit\",\n" + "        \"createdAt\": \"2020-05-18T15:08:06.602Z\",\n"
                    + "        \"endedAt\": null\n" + "    },\n" + "    {\n"
                    + "        \"id\": \"09f6e64b-63cc-41a2-8a4a-076bbd981077\",\n"
                    + "        \"tracker\": \"suunto\",\n" + "        \"createdAt\": \"2020-05-18T15:30:53.978Z\",\n"
                    + "        \"endedAt\": null\n" + "    }\n" + "]");
            mockWebServer.enqueue(mockResponse);

            ApiCallResult<TrackerConnection[]> result = sourcesService.getCurrentConnections().execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertFalse("successful result", result.isError());
            TrackerConnection[] connections = result.getValue();
            assertEquals("should have tracker name", "fitbit", connections[0].getTracker());
            assertEquals("should have tracker id", "0c98f062-055e-42fe-9c30-3a9645716d6f", connections[0].getId());
            assertEquals("should have createdAt",
                Date.from(ZonedDateTime.parse("2020-05-18T15:08:06.602Z").toInstant()),
                connections[0].getCreatedAt());
            assertNull("should not have endedAt", connections[0].getEndedAt());

            assertEquals("should have tracker name", "suunto", connections[1].getTracker());
            assertEquals("should have tracker id", "09f6e64b-63cc-41a2-8a4a-076bbd981077", connections[1].getId());
            assertEquals("should have createdAt",
                Date.from(ZonedDateTime.parse("2020-05-18T15:30:53.978Z").toInstant()),
                connections[1].getCreatedAt());
            assertNull("should not have endedAt", connections[1].getEndedAt());
        }
    }

    public static class UploadGoogleFitData extends GivenRobolectricContext {
        ActivitySourcesService sourcesService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;
        Keystore testKeystore;
        SigningKey validSigningKey;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 2);
            validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
            testKeystore = new Keystore(new InMemoryStorage());
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void uploadGoogleFitData_WithHttpOkResponseCode_RespondWithSuccessfulResult()
            throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());

            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
            mockWebServer.enqueue(mockResponse);

            GFUploadData data = new GFUploadData();
            List<GFCalorieDataPoint> calories = Arrays.asList(new GFCalorieDataPoint(5.2751f,
                Date.from(Instant.parse("2020-01-01T10:05:00Z")),
                Date.from(Instant.parse("2020-01-01T10:15:00Z")),
                "calories:googlefit"));
            data.setCaloriesData(calories);
            ApiCallResult<Void> result = sourcesService.uploadGoogleFitData(data).execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertFalse("successful result", result.isError());
            assertEquals("request's body should carry the gf data",
                "{\"caloriesData\":[{\"dataSource\":\"calories:googlefit\",\"entries\":[{\"start\":\"2020-01-01T10:05:00.000Z\",\"value\":5.2751}]}],\"hrData\":[],\"sessionsData\":[],\"stepsData\":[]}",
                request.getBody().readUtf8());
        }
    }

    public static class UpdateProfileOnBehalfOfGoogleFit extends GivenRobolectricContext {
        ActivitySourcesService sourcesService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;
        Keystore testKeystore;
        SigningKey validSigningKey;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 2);
            validSigningKey = new SigningKey(KEY_ID, SECRET_KEY, calendar.getTime());
            testKeystore = new Keystore(new InMemoryStorage());
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void updateProfileOnBehalfOfGoogleFit_WhenFullParamsAndHttpResponseCodeIsOK_RespondWithSuccessfulResult()
            throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());

            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
            mockWebServer.enqueue(mockResponse);

            GFSynchronizableProfileParams profileParams = new GFSynchronizableProfileParams();
            profileParams.setHeight(180.45f);
            profileParams.setWeight(77.77f);
            ApiCallResult<Void> result = sourcesService.updateProfileOnBehalfOfGoogleFit(profileParams).execute();
            assertFalse("successful result", result.isError());

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("request's body should carry correct json",
                "{\"height\":180.45,\"weight\":77.77}",
                request.getBody().readUtf8());
            assertEquals("PUT", request.getMethod());
            assertEquals("/sdk/activity-sources/v1/USER_TOKEN/googlefit/profile", request.getPath());
        }

        @Test
        public void updateProfileOnBehalfOfGoogleFit_WhenPartialParamsAndHttpResponseCodeIsOK_RespondWithSuccessfulResult()
            throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            sourcesService = new ActivitySourcesService(clientBuilder.build());

            MockResponse mockResponse = new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK);
            mockWebServer.enqueue(mockResponse);

            GFSynchronizableProfileParams profileParams = new GFSynchronizableProfileParams();
            profileParams.setHeight(180.45f);
            ApiCallResult<Void> result = sourcesService.updateProfileOnBehalfOfGoogleFit(profileParams).execute();
            assertFalse("successful result", result.isError());

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals("request's body should carry the correct json",
                "{\"height\":180.45}",
                request.getBody().readUtf8());
            assertEquals("PUT", request.getMethod());
            assertEquals("/sdk/activity-sources/v1/USER_TOKEN/googlefit/profile", request.getPath());
        }
    }
}
