package com.fjuul.sdk.http.interceptors;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SigningInterceptor implements Interceptor {
    private SigningKeychain keychain;

    public SigningInterceptor(SigningKeychain keychain) {
        this.keychain = keychain;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // TODO: handle case when no valid signing key in the keychain
        // TODO: handle case when current valid signing key was expired already
        // TODO: handle case when current valid signing key was rejected by the back-end side
        Optional<SigningKey> keyOptional = this.keychain.getFirstValid();
        if (!keyOptional.isPresent()) {
            throw new Error("No any valid keys to sign the request");
        }
        SigningKey key = keyOptional.get();
        Request signedRequest = signRequestByKey(chain.request(), key);
        return chain.proceed(signedRequest);
    }

    private Request signRequestByKey(Request request, SigningKey key) {
        Request.Builder signedRequestBuilder = request.newBuilder();
        String checkingRequestHeaders = this.isRequestWithDigestChecking(request) ?
            "(request-target) date digest" :
            "(request-target) date";
        String requestTargetPart = String.format(
            "(request-target): %s %s",
            request.method().toLowerCase(),
            request.url().encodedPath()
        );
        LocalDate date = LocalDate.now();
        String formattedDate = date.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        // TODO: assign date format to headers (check if retrofit do it by default) ?
        String datePart = String.format("date: %s", formattedDate);
        StringBuilder signingStringBuilder = new StringBuilder(
            String.format("%s\n%s", requestTargetPart, datePart)
        );
        if (isRequestWithDigestChecking(request)) {
            RequestBody body = request.body();
            if (body == null) {
                signingStringBuilder.append("\ndigest: ");
                signedRequestBuilder.addHeader("Digest", "");
            } else {
                String bodyDigest = this.buildDigestOfBody(body);
                String headerValue = String.format("SHA-256=%s", bodyDigest);
                String digestPart = String.format("\ndigest: %s", headerValue);
                signingStringBuilder.append(digestPart);
                signedRequestBuilder.addHeader("Digest", headerValue);
            }
        }
        String signingString = signingStringBuilder.toString();
        String signature = buildEncodedEncryptedSignature(signingString, key.getSecret());
        String signatureHeader = String.format(
            "keyId=\"%s\",algorithm=\"hmac-sha256\",headers=\"%s\",signature=\"%s\"",
            key.getId(),
            checkingRequestHeaders,
            signature
        );
        signedRequestBuilder.addHeader("Signature", signatureHeader);
        return signedRequestBuilder.build();
    }

    private String buildDigestOfBody(RequestBody body) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        byte[] stringBodyBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        byte[] hashedBodyBytes = digest.digest(stringBodyBytes);
        String encodedDigest = Base64.getEncoder().encodeToString(hashedBodyBytes);
        return encodedDigest;
    }

    private Boolean isRequestWithDigestChecking(Request request) {
        switch (request.method()) {
            case "PUT":
            case "POST":
                return true;
            default:
                return false;
        }
    }

    private String buildEncodedEncryptedSignature(String string, String key) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
            sha256HMAC.init(secretKeySpec);
            final byte[] signatureBytes = sha256HMAC.doFinal(string.getBytes(StandardCharsets.UTF_8));
            final Base64.Encoder encoder = Base64.getEncoder();
            final String result = new String(encoder.encode(signatureBytes), "UTF-8");
            return  result;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
