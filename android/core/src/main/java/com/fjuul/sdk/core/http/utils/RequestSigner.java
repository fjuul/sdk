package com.fjuul.sdk.core.http.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fjuul.sdk.core.entities.SigningKey;

import android.annotation.SuppressLint;
import android.util.Base64;
import androidx.annotation.NonNull;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class RequestSigner {
    Clock clock;

    public RequestSigner(@NonNull Clock clock) {
        this.clock = clock;
    }

    @SuppressLint("NewApi")
    public RequestSigner() {
        this.clock = Clock.systemUTC();
    }

    @NonNull
    public Request signRequestByKey(@NonNull Request request, @NonNull SigningKey key) {
        Request.Builder signedRequestBuilder = request.newBuilder();
        String checkingRequestHeaders =
            this.isRequestWithDigestChecking(request) ? "(request-target) date digest" : "(request-target) date";
        StringBuilder encodedRequestTargetBuilder =
            new StringBuilder(String.format("%s %s", request.method().toLowerCase(), request.url().encodedPath()));
        String encodedQuery = request.url().encodedQuery();
        if (encodedQuery != null) {
            encodedRequestTargetBuilder.append("?").append(encodedQuery);
        }
        String encodedFragment = request.url().encodedFragment();
        if (encodedFragment != null) {
            encodedRequestTargetBuilder.append("#").append(encodedFragment);
        }

        String requestTargetPart = String.format("(request-target): %s", encodedRequestTargetBuilder.toString());

        @SuppressLint({"NewApi", "LocalSuppress"})
        Instant instant = Instant.now(clock);
        @SuppressLint({"NewApi", "LocalSuppress"})
        OffsetDateTime offset = instant.atOffset(ZoneOffset.UTC);
        @SuppressLint({"NewApi", "LocalSuppress"})
        String formattedDate = offset.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        signedRequestBuilder.header("Date", formattedDate);

        String datePart = String.format("date: %s", formattedDate);
        StringBuilder signingStringBuilder = new StringBuilder(String.format("%s\n%s", requestTargetPart, datePart));
        if (isRequestWithDigestChecking(request)) {
            RequestBody body = request.body();
            if (body == null || RequestSigner.requestBodyToString(body).isEmpty()) {
                signingStringBuilder.append("\ndigest: ");
                signedRequestBuilder.header("Digest", "");
            } else {
                String bodyDigest = this.buildDigestOfBody(body);
                String headerValue = String.format("SHA-256=%s", bodyDigest);
                String digestPart = String.format("\ndigest: %s", headerValue);
                signingStringBuilder.append(digestPart);
                signedRequestBuilder.header("Digest", headerValue);
            }
        }
        String signingString = signingStringBuilder.toString();
        String signature = buildEncodedEncryptedSignature(signingString, key.getSecret());
        String signatureHeader = String.format("keyId=\"%s\",algorithm=\"hmac-sha256\",headers=\"%s\",signature=\"%s\"",
            key.getId(), checkingRequestHeaders, signature);
        signedRequestBuilder.header("Signature", signatureHeader);
        return signedRequestBuilder.build();
    }

    private String buildDigestOfBody(RequestBody body) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        byte[] stringBodyBytes = RequestSigner.requestBodyToString(body).getBytes(StandardCharsets.UTF_8);
        byte[] hashedBodyBytes = digest.digest(stringBodyBytes);
        String encodedDigest = Base64.encodeToString(hashedBodyBytes, Base64.NO_WRAP);
        return encodedDigest;
    }

    private static String requestBodyToString(final RequestBody body) {
        try {
            final RequestBody copy = body;
            final Buffer buffer = new Buffer();
            if (copy != null) {
                copy.writeTo(buffer);
            } else {
                return "";
            }
            return buffer.readUtf8();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private Boolean isRequestWithDigestChecking(Request request) {
        return Arrays.asList("PUT", "POST").contains(request.method());
    }

    private String buildEncodedEncryptedSignature(String string, String key) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            final byte[] signatureBytes = sha256HMAC.doFinal(string.getBytes(StandardCharsets.UTF_8));
            final String result = new String(Base64.encode(signatureBytes, Base64.NO_WRAP), "UTF-8");
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
