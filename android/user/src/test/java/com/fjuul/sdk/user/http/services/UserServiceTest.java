package com.fjuul.sdk.user.http.services;

import android.os.Build;

import com.fjuul.sdk.fixtures.http.TestApiClient;
import com.fjuul.sdk.http.utils.ApiCallResult;
import com.fjuul.sdk.user.entities.Gender;
import com.fjuul.sdk.user.entities.UserCreationResult;
import com.fjuul.sdk.user.entities.UserProfile;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    @RunWith(RobolectricTestRunner.class)
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
    }
}
