package com.fjuul.sdk.user.http.services;

import android.os.Build;

import com.fjuul.sdk.entities.InMemoryStorage;
import com.fjuul.sdk.entities.Keystore;
import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.UserCredentials;
import com.fjuul.sdk.errors.ApiErrors;
import com.fjuul.sdk.fixtures.http.TestApiClient;
import com.fjuul.sdk.http.utils.ApiCallResult;
import com.fjuul.sdk.user.entities.Gender;
import com.fjuul.sdk.user.entities.UserCreationResult;
import com.fjuul.sdk.user.entities.UserProfile;

import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;

import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
@Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
public class UserServiceTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";
    static final String USER_TOKEN = "USER_TOKEN";
    static final String USER_SECRET = "USER_TOKEN";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {
    }

    public static class CreateUserTest extends GivenRobolectricContext {
        UserService userService;
        MockWebServer mockWebServer;
        TestApiClient.Builder clientBuilder;

        @Before
        public void setup() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void createUser_ValidParams_RespondWithCreationResult() throws IOException, InterruptedException {
            userService = new UserService(clientBuilder.build());
            MockResponse mockResponse =
                new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\n" +
                        "  \"user\": {\n" +
                        "    \"token\": \"2e573e83-88f0-476f-bb16-a076efe53659\",\n" +
                        "    \"height\": 180,\n" +
                        "    \"weight\": 68,\n" +
                        "    \"gender\": \"female\",\n" +
                        "    \"birthDate\": \"1980-06-01\",\n" +
                        "    \"timezone\": \"Europe/Helsinki\",\n" +
                        "    \"locale\": \"fi\"\n" +
                        "  },\n" +
                        "  \"secret\": \"user_secret\"\n" +
                        "}");
            mockWebServer.enqueue(mockResponse);

            UserProfile.PartialBuilder userBuilder = new UserProfile.PartialBuilder();
            userBuilder.setHeight(180);
            userBuilder.setWeight(68);
            userBuilder.setGender(Gender.female);
            userBuilder.setBirthDate(LocalDate.of(1980, 06, 01));
            userBuilder.setTimezone(TimeZone.getTimeZone("Europe/Helsinki"));
            userBuilder.setLocale("fi");
            ApiCallResult<UserCreationResult> result = userService.createUser(userBuilder).execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertEquals(
                "transforms user params to json",
                "{\"birthDate\":\"1980-06-01\",\"gender\":\"female\",\"height\":180.0,\"locale\":\"fi\",\"timezone\":\"Europe/Helsinki\",\"weight\":68.0}",
                request.getBody().readUtf8());

            assertFalse("success result", result.isError());
            assertEquals("should have user secret", "user_secret", result.getValue().getSecret());
            UserProfile profile = result.getValue().getUser();
            assertEquals("should have user profile", "2e573e83-88f0-476f-bb16-a076efe53659", profile.getToken());
            assertEquals(180f, profile.getHeight(), 0.0001);
            assertEquals(68f, profile.getWeight(), 0.0001);
            assertEquals(Gender.female, profile.getGender());
            assertEquals(LocalDate.of(1980, 06, 01), profile.getBirthDate());
            assertEquals(TimeZone.getTimeZone("Europe/Helsinki"), profile.getTimezone());
            assertEquals("fi", profile.getLocale());
        }

        @Test
        public void createUser_InvalidParams_RespondWithError() throws IOException, InterruptedException {
            userService = new UserService(clientBuilder.build());
            MockResponse mockResponse =
                new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\n" +
                        "  \"message\":\"Bad Request: Validation error\",\n" +
                        "  \"errors\":[\n" +
                        "    {\n" +
                        "      \"property\":\"timezone\",\n" +
                        "      \"children\":[\n" +
                        "\n" +
                        "      ],\n" +
                        "      \"constraints\":{\n" +
                        "        \"isNotEmpty\":\"timezone should not be empty\"\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}");
            mockWebServer.enqueue(mockResponse);

            UserProfile.PartialBuilder userBuilder = new UserProfile.PartialBuilder();
            userBuilder.setHeight(180);
            userBuilder.setWeight(68);
            userBuilder.setGender(Gender.female);
            userBuilder.setBirthDate(LocalDate.of(1980, 06, 01));
            userBuilder.setLocale("fi");
            ApiCallResult<UserCreationResult> result = userService.createUser(userBuilder).execute();
            RecordedRequest request = mockWebServer.takeRequest();

            assertEquals(
                "transforms user params to json",
                "{\"birthDate\":\"1980-06-01\",\"gender\":\"female\",\"height\":180.0,\"locale\":\"fi\",\"weight\":68.0}",
                request.getBody().readUtf8());

            assertTrue("unsuccessful result", result.isError());
            ApiErrors.CommonError error = result.getError();
            assertThat(error, instanceOf(ApiErrors.BadRequestError.class));
            assertEquals("should have error message", "Bad Request: Validation error", error.getMessage());
        }
    }

    public static class GetProfileTest extends GivenRobolectricContext {
        UserService userService;
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
            testKeystore = new Keystore(new InMemoryStorage(), USER_TOKEN);
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void getProfile_WithoutUserCredentials_ThrowsError() throws IOException, InterruptedException {
            userService = new UserService(clientBuilder.build());
            try {
                ApiCallResult<UserProfile> result = userService.getProfile().execute();
                assertTrue("should throws exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalStateException.class));
                assertEquals("should have error message", "The builder needed user credentials to build a signing client", exc.getMessage());
            }
        }

        @Test
        public void getProfile_WithValidUserCredentials_RespondsWithProfile() {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            userService = new UserService(clientBuilder.build());
            MockResponse mockResponse =
                new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\n" +
                        "  \"token\": \"2e573e83-88f0-476f-bb16-a076efe53659\",\n" +
                        "  \"height\": 186,\n" +
                        "  \"weight\": 75,\n" +
                        "  \"gender\": \"male\",\n" +
                        "  \"birthDate\": \"1980-12-31\",\n" +
                        "  \"locale\": \"en\",\n" +
                        "  \"timezone\": \"Europe/Moscow\"\n" +
                        "}");
            mockWebServer.enqueue(mockResponse);
            ApiCallResult<UserProfile> result = userService.getProfile().execute();
            assertFalse("success result", result.isError());
            UserProfile profile = result.getValue();
            assertEquals("should have user profile", "2e573e83-88f0-476f-bb16-a076efe53659", profile.getToken());
            assertEquals(186f, profile.getHeight(), 0.0001);
            assertEquals(75f, profile.getWeight(), 0.0001);
            assertEquals(Gender.male, profile.getGender());
            assertEquals(LocalDate.of(1980, 12, 31), profile.getBirthDate());
            assertEquals(TimeZone.getTimeZone("Europe/Moscow"), profile.getTimezone());
            assertEquals("en", profile.getLocale());
        }
    }

    public static class UpdateProfileTest extends GivenRobolectricContext {
        UserService userService;
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
            testKeystore = new Keystore(new InMemoryStorage(), USER_TOKEN);
            clientBuilder = new TestApiClient.Builder(mockWebServer);
        }

        @After
        public void teardown() throws IOException {
            mockWebServer.shutdown();
        }

        @Test
        public void updateProfile_WithoutUserCredentials_ThrowsError() throws IOException, InterruptedException {
            userService = new UserService(clientBuilder.build());
            try {
                UserProfile.PartialBuilder profileBuilder = new UserProfile.PartialBuilder();
                profileBuilder.setHeight(120);
                ApiCallResult<UserProfile> result = userService.updateProfile(profileBuilder).execute();
                assertTrue("should throws exception", false);
            } catch (Exception exc) {
                assertThat(exc, instanceOf(IllegalStateException.class));
                assertEquals("should have error message", "The builder needed user credentials to build a signing client", exc.getMessage());
            }
        }

        @Test
        public void updateProfile_ValidUserParams_RespondsWithProfile() throws InterruptedException {
            clientBuilder.setUserCredentials(new UserCredentials(USER_TOKEN, USER_SECRET));
            testKeystore.setKey(validSigningKey);
            clientBuilder.setKeystore(testKeystore);
            userService = new UserService(clientBuilder.build());
            MockResponse mockResponse =
                new MockResponse().setResponseCode(HttpURLConnection.HTTP_OK)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\n" +
                        "  \"token\": \"2e573e83-88f0-476f-bb16-a076efe53659\",\n" +
                        "  \"height\": 120,\n" +
                        "  \"weight\": 75,\n" +
                        "  \"gender\": \"male\",\n" +
                        "  \"birthDate\": \"1980-12-31\",\n" +
                        "  \"locale\": \"en\",\n" +
                        "  \"timezone\": \"Europe/Moscow\"\n" +
                        "}");
            mockWebServer.enqueue(mockResponse);
            UserProfile.PartialBuilder profileBuilder = new UserProfile.PartialBuilder();
            profileBuilder.setHeight(120);
            ApiCallResult<UserProfile> result = userService.updateProfile(profileBuilder).execute();

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals(
                "transforms only given user params to json",
                "{\"height\":120.0}",
                request.getBody().readUtf8());

            assertFalse("success result", result.isError());
            UserProfile profile = result.getValue();
            assertEquals("should have user profile", "2e573e83-88f0-476f-bb16-a076efe53659", profile.getToken());
            assertEquals(120f, profile.getHeight(), 0.0001);
            assertEquals(75f, profile.getWeight(), 0.0001);
            assertEquals(Gender.male, profile.getGender());
            assertEquals(LocalDate.of(1980, 12, 31), profile.getBirthDate());
            assertEquals(TimeZone.getTimeZone("Europe/Moscow"), profile.getTimezone());
            assertEquals("en", profile.getLocale());
        }
    }
}