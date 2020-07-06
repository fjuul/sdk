package com.fjuul.sdk.http.interceptors;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.services.UserSigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;
import retrofit2.adapter.rxjava2.Result;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class SigningInterceptorTest {

    @Test
    public void intercept_EmptyKeychainWithFailedIssueResult_throwsException() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        SigningKeychain testKeychain = new SigningKeychain();
        UserSigningService mockedSigningService = mock(UserSigningService.class);
        Result<Response<SigningKey>> failedSigningKeyResult = Result.response(
            Response.error(
                401,
                ResponseBody.create(
                    MediaType.get("application/json"),
                    "{ \"message\": \"error message\", \"errorCode\": \"expired_signing_key\" }")));
        doReturn(Observable.just(failedSigningKeyResult)).when(mockedSigningService).issueKey();
        SigningInterceptor interceptor = new SigningInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addInterceptor(interceptor)
            .build();

        try {
            okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build()).execute();
            assertEquals("fails", true, false);
        } catch (IOException exc) {
            assertEquals(
                "throws io-exception with message",
                "Couldn't retrieve a signing key",
                exc.getMessage());
        } finally {
            mockWebServer.shutdown();
        }

    }

    @Test
    public void intercept_KeychainWithExpiredKeyAndSuccessIssueResult_returnResponse() throws Exception {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        SigningKey testExpiredSigningKey = new SigningKey("expired-key-id", "TOP_SECRET", new Date(),false);
        SigningKeychain testKeychain = new SigningKeychain(testExpiredSigningKey);

        SigningKey newValidSigningKey = new SigningKey("valid-key-id", "TOP_SECRET1", new Date());
        UserSigningService mockedSigningService = mock(UserSigningService.class);
        Result<SigningKey> succeedSigningKeyResult = Result.response(Response.success(200, newValidSigningKey));
        doReturn(Observable.just(succeedSigningKeyResult)).when(mockedSigningService).issueKey();
        SigningInterceptor interceptor = new SigningInterceptor(testKeychain, new RequestSigner(), mockedSigningService);
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .addInterceptor(interceptor)
            .build();

        okHttpClient.newCall(new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build()).execute();
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat("carries new key-id",
            request.getHeader("Signature"),
            CoreMatchers.containsString("valid-key-id"));
        mockWebServer.shutdown();
    }
}
