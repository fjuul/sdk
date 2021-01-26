package com.fjuul.sdk.core.http.interceptors;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.os.Build;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.P})
public class SDKUserAgentInterceptorTest {
    @Test
    public void intercept() throws IOException, InterruptedException {
        final MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start();
        mockWebServer.enqueue(new MockResponse());

        final SDKUserAgentInterceptor interceptor = new SDKUserAgentInterceptor("1.0.5");
        final OkHttpClient okHttpClient = new OkHttpClient().newBuilder().addInterceptor(interceptor).build();
        final Request originalRequest = new Request.Builder().url(mockWebServer.url("/sdk/v1/analytics")).build();
        okHttpClient.newCall(originalRequest).execute();
        final RecordedRequest interceptedRequest = mockWebServer.takeRequest();
        assertEquals("interceptor should set user-agent",
            "Fjuul-Android-SDK/1.0.5",
            interceptedRequest.getHeader("User-Agent"));
        mockWebServer.shutdown();
    }
}
