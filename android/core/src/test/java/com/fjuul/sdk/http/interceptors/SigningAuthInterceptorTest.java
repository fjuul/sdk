package com.fjuul.sdk.http.interceptors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.entities.InMemoryStorage;
import com.fjuul.sdk.entities.Keystore;
import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import android.os.Build;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
public class SigningAuthInterceptorTest {
    static final String USER_TOKEN = "USER_TOKEN";

    @Test
    public void intercept_EmptyKeystoreWithFailedIssueResult_returnIssueResponse() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        Keystore testKeychain = new Keystore(new InMemoryStorage(), USER_TOKEN);
        UserSigningService mockedSigningService = mock(UserSigningService.class, Mockito.RETURNS_DEEP_STUBS);
        Request outboundRequest = new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build();
        okhttp3.Response incomingRawResponse =
            new okhttp3.Response.Builder().header("x-authentication-error", "wrong_credentials")
                .code(HttpURLConnection.HTTP_UNAUTHORIZED)
                .request(outboundRequest)
                .protocol(Protocol.HTTP_1_1)
                .message("Unauthorized")
                .build();
        Response mockedSigningKeyResponse = Response.error(ResponseBody.create(MediaType.get("application/json"),
            "{ \"message\": \"error message\", \"errorCode\": \"expired_signing_key\" }"), incomingRawResponse);

        when(mockedSigningService.issueKey().execute()).thenReturn(mockedSigningKeyResponse);
        SigningAuthInterceptor interceptorWithAuthenticator =
            new SigningAuthInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addInterceptor(interceptorWithAuthenticator)
            .authenticator(interceptorWithAuthenticator)
            .build();
        okhttp3.Response returnedResponse = okHttpClient.newCall(outboundRequest).execute();
        assertEquals("returns raw response of the issue request", mockedSigningKeyResponse.errorBody(),
            returnedResponse.body());
        assertEquals("returns raw response of the issue request", mockedSigningKeyResponse.raw().message(),
            returnedResponse.message());
        assertEquals("returns raw response of the issue request", mockedSigningKeyResponse.raw().code(),
            returnedResponse.code());
        mockWebServer.shutdown();
    }

    @Test
    public void intercept_KeystoreWithExpiredKeyAndSuccessIssueResult_returnResponse() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        SigningKey testExpiredSigningKey = new SigningKey("expired-key-id", "TOP_SECRET", new Date());
        Keystore testKeychain = new Keystore(new InMemoryStorage(), USER_TOKEN);
        testKeychain.setKey(testExpiredSigningKey);

        SigningKey newValidSigningKey = new SigningKey("valid-key-id", "TOP_SECRET1", new Date());
        UserSigningService mockedSigningService = mock(UserSigningService.class, Mockito.RETURNS_DEEP_STUBS);
        Response mockedSigningKeyResponse = Response.success(HttpURLConnection.HTTP_OK, newValidSigningKey);
        when(mockedSigningService.issueKey().execute()).thenReturn(mockedSigningKeyResponse);
        SigningAuthInterceptor interceptorWithAuthenticator =
            new SigningAuthInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addInterceptor(interceptorWithAuthenticator)
            .authenticator(interceptorWithAuthenticator)
            .build();

        okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build()).execute();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat("carries new key-id", request.getHeader("Signature"), CoreMatchers.containsString("valid-key-id"));
        mockWebServer.shutdown();
    }

    @Test
    public void intercept_SimultaneousCallsWithEmptyKeystore_requestIssueKeyOnlyOnce()
        throws IOException, InterruptedException {
        final int THREAD_POOL_SIZE = 5;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        Keystore testKeychain = new Keystore(new InMemoryStorage(), USER_TOKEN);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        Date expiresAt = calendar.getTime();
        SigningKey newValidSigningKey = new SigningKey("valid-key-id", "TOP_SECRET1", expiresAt);
        UserSigningService mockedSigningService = mock(UserSigningService.class, Mockito.RETURNS_DEEP_STUBS);
        Response mockedSigningKeyResponse = Response.success(HttpURLConnection.HTTP_OK, newValidSigningKey);
        when(mockedSigningService.issueKey().execute()).thenReturn(mockedSigningKeyResponse);
        SigningAuthInterceptor interceptorWithAuthenticator =
            new SigningAuthInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addInterceptor(interceptorWithAuthenticator)
            .authenticator(interceptorWithAuthenticator)
            .build();

        // enqueue successful responses (it's assumed that issue response will be succeeded)
        IntStream.rangeClosed(1, THREAD_POOL_SIZE).forEach(i -> {
            mockWebServer.enqueue(new MockResponse());
        });
        // make parallel requests
        IntStream.rangeClosed(1, THREAD_POOL_SIZE).forEach(i -> {
            executor.submit(() -> {
                try {
                    okhttp3.Response response =
                        okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build())
                            .execute();
                } catch (IOException e) {
                    assertFalse("must no catch here", true);
                }
            });
        });
        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);
        // NOTE: 1+1 is for case when we mock this method by self
        verify(mockedSigningService, times(2)).issueKey();
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            // check all outbound requests from threads
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat("each request carries new key-id", request.getHeader("Signature"),
                CoreMatchers.containsString("valid-key-id"));
        }
    }

    @Test
    public void intercept_SimultaneousCallsWithInvalidatedKeyResponses_requestIssueKeyOnlyOnce()
        throws IOException, InterruptedException {
        final int THREAD_POOL_SIZE = 5;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 2);
        Date expiresAt = calendar.getTime();

        SigningKey signingKey = new SigningKey("previous-key-id", "TOP_SECRET", expiresAt);
        Keystore testKeychain = new Keystore(new InMemoryStorage(), USER_TOKEN);
        testKeychain.setKey(signingKey);

        UserSigningService mockedSigningService = mock(UserSigningService.class, Mockito.RETURNS_DEEP_STUBS);
        SigningKey newValidSigningKey = new SigningKey("valid-key-id", "TOP_SECRET1", expiresAt);
        Response mockedSigningKeyResponse = Response.success(HttpURLConnection.HTTP_OK, newValidSigningKey);
        when(mockedSigningService.issueKey().execute()).thenReturn(mockedSigningKeyResponse);
        SigningAuthInterceptor interceptorWithAuthenticator =
            new SigningAuthInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addNetworkInterceptor(interceptorWithAuthenticator)
            .authenticator(interceptorWithAuthenticator)
            .build();

        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                if (request.getHeader("Signature").contains("previous-key-id")) {
                    // unauthorized response for the previous key
                    return new MockResponse().setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                        .addHeader("x-authentication-error", "invalid_key_id")
                        .setBody("{\"message\":\"Unauthorized: key not found\"}");
                } else if (request.getHeader("Signature").contains("valid-key-id")) {
                    // successful response with new key
                    return new MockResponse();
                }
                return null;
            }
        });
        // make parallel requests
        IntStream.rangeClosed(1, THREAD_POOL_SIZE).forEach(i -> {
            executor.submit(() -> {
                try {
                    okhttp3.Response response =
                        okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build())
                            .execute();
                } catch (IOException e) {
                    assertFalse("must no catch here", true);
                }
            });
        });
        executor.shutdown();
        executor.awaitTermination(15, TimeUnit.SECONDS);

        int updatedRequestsCounter = 0;
        for (int i = 0; i < THREAD_POOL_SIZE * 2 && updatedRequestsCounter < THREAD_POOL_SIZE; i++) {
            RecordedRequest request = mockWebServer.takeRequest();
            final String signatureHeader = request.getHeader("Signature");
            if (signatureHeader.contains("valid-key-id")) {
                updatedRequestsCounter += 1;
            }
        }
        assertEquals("all requests finally carry new key-id", THREAD_POOL_SIZE, updatedRequestsCounter);
        // NOTE: 1+1 is for case when we mock this method by self
        verify(mockedSigningService, times(2)).issueKey();
    }
}