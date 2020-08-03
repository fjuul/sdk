package com.fjuul.sdk.http.interceptors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fjuul.sdk.entities.Keystore;
import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class SigningAuthInterceptor implements Interceptor, Authenticator {
    private Keystore keystore;
    private RequestSigner requestSigner;
    private ISigningService signingService;
    private final Pattern signatureHeaderKeyIdPattern = Pattern.compile("keyId=\"(.+?)\"");

    public SigningAuthInterceptor(@NonNull Keystore keystore, @NonNull RequestSigner requestSigner,
        @NonNull ISigningService signingService) {
        this.keystore = keystore;
        this.requestSigner = requestSigner;
        this.signingService = signingService;
    }

    @SuppressLint({"NewApi", "UnknownNullness"})
    @Override
    public Response intercept(Chain chain) throws IOException {
        // TODO: handle case when current valid signing key was expired already
        Optional<SigningKey> keyOptional = this.keystore.getValidKey();
        SigningKey signingKey = null;
        if (!keyOptional.isPresent()) {
            // need to request a new signing key
            synchronized (this) {
                // double check if we have now new signing key
                keyOptional = this.keystore.getValidKey();
                // if still no valid key => request new one
                if (!keyOptional.isPresent()) {
                    retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
                    if (newKeyResponse.isSuccessful()) {
                        SigningKey newKey = newKeyResponse.body();
                        keystore.setKey(newKey);
                        signingKey = newKey;
                    } else {
                        // return response of a request of the signing key to infer http error
                        return extractOriginalRawResponse(newKeyResponse);
                    }
                } else {
                    signingKey = keyOptional.get();
                }
            }
        } else {
            signingKey = keyOptional.get();
        }
        Request signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
        return chain.proceed(signedRequest);
    }

    @SuppressLint({"NewApi", "UnknownNullness"})
    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // give up if retry count is more than 1
        if (responseCount(response) > 1) {
            return null;
        }

        // NOTE: try to retrieve a signing key once if error_code is `expired_signing_key` or
        // 'invalid_key_id'
        String authenticationErrorCode = response.header("x-authentication-error");
        if (!Arrays.asList("invalid_key_id", "expired_signing_key").contains(authenticationErrorCode)) {
            return null;
        }
        Request request = getRelatedRequest(response);
        if (request == null) {
            return null;
        }

        synchronized (this) {
            // NOTE: double check if we have now new signing key
            // (it could be possible since this code block is synchronized)
            Optional<SigningKey> keyOptional = this.keystore.getValidKey();
            Matcher matcher = signatureHeaderKeyIdPattern.matcher(request.header("Signature"));
            matcher.find();
            String keyIdMatch = matcher.group(1);
            if (keyOptional.isPresent() && !keyOptional.get().getId().equals(keyIdMatch)) {
                SigningKey key = keyOptional.get();
                return requestSigner.signRequestByKey(response.request(), key);
            }
            // if still no valid key => request new one
            retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
            if (newKeyResponse.isSuccessful()) {
                SigningKey newKey = newKeyResponse.body();
                keystore.setKey(newKey);
                return requestSigner.signRequestByKey(response.request(), newKey);
            } else {
                return null;
            }
        }
    }

    private retrofit2.Response<SigningKey> issueNewKey() throws IOException {
        return signingService.issueKey().execute();
    }

    private <T> Response extractOriginalRawResponse(retrofit2.Response<T> response) {
        return response.raw().newBuilder().body(response.errorBody()).build();
    }

    private Request getRelatedRequest(Response response) {
        // NOTE: for some weird reasons, in test environment request() method returns
        // request with empty headers
        Request request = response.request();
        if (request.headers().size() == 0 && response.networkResponse() != null) {
            request = response.networkResponse().request();
        }
        if (request.headers().size() == 0 && response.cacheResponse() != null) {
            request = response.cacheResponse().request();
        }
        if (request.headers().size() == 0) {
            return null;
        }
        return request;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
