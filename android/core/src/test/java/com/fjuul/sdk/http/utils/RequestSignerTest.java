package com.fjuul.sdk.http.utils;

import com.fjuul.sdk.entities.SigningKey;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.*;


import okhttp3.Request;

import static org.junit.Assert.*;

@RunWith(Enclosed.class)
public class RequestSignerTest {


    public static class GetRequestTest {
        final String SECRET_KEY = "REAL_SECRET_KEY";
        final String KEY_ID = "signing-key-id-1234";
        Clock fixedClock;

        @Before
        public void beforeTests() {
            String instantExpected = "2020-02-13T15:56:23Z";
            fixedClock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));
        }

        @Test
        public void signRequest_WithGetMethod_returnsSignedRequest() throws InterruptedException {
            Request.Builder testRequestBuilder = new Request.Builder();
            Request testRequest = testRequestBuilder
                .url("https://fjuul.dev.api/analytics/v1/dailyStats/userToken/2020-01-15")
                .get()
                .build();
            SigningKey key = mock(SigningKey.class);
            when(key.getSecret()).thenReturn(SECRET_KEY);
            when(key.getId()).thenReturn(KEY_ID);
            RequestSigner subject = new RequestSigner(fixedClock);

            final Request signedRequest = subject.signRequestByKey(testRequest, key);
            assertEquals(
                "request has correct signature header",
                signedRequest.header("Signature"),
                "keyId=\"signing-key-id-1234\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"tu8E+96kyaexTmJ7Oep4Ds4bDFYE5ZdDWafqS8yEd20=\"")
            ;
            assertEquals(
                "request has a date header",
                signedRequest.header("Date"),
                "Thu, 13 Feb 2020 15:56:23 GMT"
            );
        }
    }
}
