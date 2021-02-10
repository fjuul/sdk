package com.fjuul.sdk.core.http.interceptors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fjuul.sdk.core.entities.Keystore;
import com.fjuul.sdk.core.entities.SigningKey;
import com.fjuul.sdk.core.http.services.ISigningService;
import com.fjuul.sdk.core.http.utils.RequestSigner;
import com.fjuul.sdk.core.utils.FjuulSDKLogger;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class SigningAuthInterceptor implements Interceptor, Authenticator {
    private final Keystore keystore;
    private final RequestSigner requestSigner;
    private final ISigningService signingService;
    private final Pattern signatureHeaderKeyIdPattern = Pattern.compile("keyId=\"(.+?)\"");

    public SigningAuthInterceptor(@NonNull Keystore keystore,
        @NonNull RequestSigner requestSigner,
        @NonNull ISigningService signingService) {
        this.keystore = keystore;
        this.requestSigner = requestSigner;
        this.signingService = signingService;
    }

    @SuppressLint({"NewApi", "UnknownNullness"})
    @Override
    public Response intercept(Chain chain) throws IOException {
        Optional<SigningKey> keyOptional = this.keystore.getValidKey();
        SigningKey signingKey;
        if (!keyOptional.isPresent()) {
            // need to request a new signing key
            synchronized (this) {
                // double check if we have now new signing key
                keyOptional = this.keystore.getValidKey();
                // if still no valid key => request new one
                if (!keyOptional.isPresent()) {
                    FjuulSDKLogger.get().d("Retrieving a new signing key");
                    final retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
                    if (newKeyResponse.isSuccessful()) {
                        final SigningKey newKey = newKeyResponse.body();
                        keystore.setKey(newKey);
                        signingKey = newKey;
                    } else {
                        FjuulSDKLogger.get().d("Failed to retrieve the signing key");
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
        final Request signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
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
        final String authenticationErrorCode = response.header("x-authentication-error");
        if (!Arrays.asList("invalid_key_id", "expired_signing_key").contains(authenticationErrorCode)) {
            return null;
        }
        final Request request = getRelatedRequest(response);
        if (request == null) {
            return null;
        }

        synchronized (this) {
            // NOTE: double check if we have now new signing key
            // (it could be possible since this code block is synchronized)
            final Optional<SigningKey> keyOptional = this.keystore.getValidKey();
            final Matcher matcher = signatureHeaderKeyIdPattern.matcher(request.header("Signature"));
            matcher.find();
            final String keyIdMatch = matcher.group(1);
            if (keyOptional.isPresent() && !keyOptional.get().getId().equals(keyIdMatch)) {
                final SigningKey key = keyOptional.get();
                return requestSigner.signRequestByKey(response.request(), key);
            }
            // if still no valid key => request new one
            FjuulSDKLogger.get().d("Retrieving a new signing key");
            final retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
            if (newKeyResponse.isSuccessful()) {
                final SigningKey newKey = newKeyResponse.body();
                keystore.setKey(newKey);
                return requestSigner.signRequestByKey(response.request(), newKey);
            } else {
                FjuulSDKLogger.get().d("Failed to retrieve the signing key");
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
