package com.fjuul.sdk.core.http.utils;

import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.fjuul.sdk.core.entities.SigningKey;

import android.os.Build;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@RunWith(Enclosed.class)
public class RequestSignerTest {
    static final String SECRET_KEY = "REAL_SECRET_KEY";
    static final String KEY_ID = "signing-key-id-1234";

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest = Config.NONE, sdk = {Build.VERSION_CODES.P})
    public abstract static class GivenRobolectricContext {}

    public static class RequestWithoutDigestTest extends GivenRobolectricContext {
        Clock fixedClock;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-02-13T15:56:23Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
        }

        @Test
        public void signRequest_WithGetMethod_returnsSignedRequest() throws InterruptedException {
            Request.Builder testRequestBuilder = new Request.Builder();
            Request testRequest =
                testRequestBuilder.url("https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15")
                    .get()
                    .build();
            SigningKey key = new SigningKey(KEY_ID, SECRET_KEY, new Date());
            RequestSigner subject = new RequestSigner(fixedClock);

            final Request signedRequest = subject.signRequestByKey(testRequest, key);
            assertEquals("request has correct signature header",
                "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"tu8E+96kyaexTmJ7Oep4Ds4bDFYE5ZdDWafqS8yEd20=\"",
                signedRequest.header("Signature"));
            assertEquals("request has a date header", "Thu, 13 Feb 2020 15:56:23 GMT", signedRequest.header("Date"));
        }

        @Test
        public void signRequest_WithGetMethodAndQueryParams_returnsSignedRequest() throws InterruptedException {
            Request.Builder testRequestBuilder = new Request.Builder();
            Request testRequest = testRequestBuilder
                .url("https://fjuul.dev.api/analytics/v1/dailyStats/userToken?startDate=2020-01-15&endDate=2020-01-20")
                .get()
                .build();
            SigningKey key = new SigningKey(KEY_ID, SECRET_KEY, new Date());
            RequestSigner subject = new RequestSigner(fixedClock);

            final Request signedRequest = subject.signRequestByKey(testRequest, key);
            assertEquals("request has correct signature header",
                "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"MOqamiGFYfhqxswMDto4zzB/1UrBJukoWIQGqN+N9vg=\"",
                signedRequest.header("Signature"));
            assertEquals("request has a date header", "Thu, 13 Feb 2020 15:56:23 GMT", signedRequest.header("Date"));
        }
    }

    public static class RequestWithDigestTest extends GivenRobolectricContext {
        Clock fixedClock;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-02-13T15:56:23Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
        }

        @Test
        public void signRequest_PostWithoutRequestBody_returnsSignedRequest() throws InterruptedException {
            Request.Builder testRequestBuilder = new Request.Builder();
            Request testRequest =
                testRequestBuilder.url("https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15")
                    .post(RequestBody.create(null, ""))
                    .build();
            SigningKey key = new SigningKey(KEY_ID, SECRET_KEY, new Date());
            RequestSigner subject = new RequestSigner(fixedClock);

            final Request signedRequest = subject.signRequestByKey(testRequest, key);
            assertEquals("request has correct signature header",
                "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date digest\",signature=\"Mmyp9dkZcBG/7Bk3okAExqvKS/E7bAOyanfAbrdAUnA=\"",
                signedRequest.header("Signature"));
            assertEquals("request has a date header", "Thu, 13 Feb 2020 15:56:23 GMT", signedRequest.header("Date"));
            assertEquals("request has an empty digest header", "", signedRequest.header("Digest"));
        }

        @Test
        public void signRequest_PostWithJsonRequestBody_returnsSignedRequest() throws InterruptedException {
            Request.Builder testRequestBuilder = new Request.Builder();
            Request testRequest =
                testRequestBuilder.url("https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15")
                    .post(
                        RequestBody.create(MediaType.get("application/json"), "{\"hello\":\"world\",\"foo\":\"bar\"}"))
                    .build();
            SigningKey key = new SigningKey(KEY_ID, SECRET_KEY, new Date());
            RequestSigner subject = new RequestSigner(fixedClock);

            final Request signedRequest = subject.signRequestByKey(testRequest, key);
            assertEquals("request has correct signature header",
                "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date digest\",signature=\"78ygswe54lAGd24/ksNjNXuZ9JNrMTI4E9TsqHaLjaU=\"",
                signedRequest.header("Signature"));
            assertEquals("request has a date header", "Thu, 13 Feb 2020 15:56:23 GMT", signedRequest.header("Date"));
            assertEquals("request has a digest header",
                "SHA-256=Q95/OQtk+2T6qHbUBZyTr/JITn+2qDMFeqAKJee0Uz0=",
                signedRequest.header("Digest"));
        }
    }
}
