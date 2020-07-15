package com.fjuul.sdk.http.interceptors;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SigningInterceptor implements Interceptor {
    private SigningKeychain keychain;
    private RequestSigner requestSigner;
    private ISigningService signingService;

    public SigningInterceptor(
            SigningKeychain keychain, RequestSigner requestSigner, ISigningService signingService) {
        this.keychain = keychain;
        this.requestSigner = requestSigner;
        this.signingService = signingService;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // TODO: handle case when current valid signing key was expired already
        Optional<SigningKey> keyOptional = this.keychain.getFirstValid();
        SigningKey signingKey = null;
        if (!keyOptional.isPresent()) {
            retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
            if (newKeyResponse.isSuccessful()) {
                SigningKey newKey = newKeyResponse.body();
                keychain.appendKey(newKey);
                signingKey = newKey;
            } else {
                // return response of a request of signing key to infer http error
                return newKeyResponse.raw();
            }
        } else {
            signingKey = keyOptional.get();
        }

        Request signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
        Response response = chain.proceed(signedRequest);
        if (response.isSuccessful() || response.code() != 401) {
            return response;
        }

        // NOTE: try to retrieve a signing key once if error_code is `expired_signing_key` or
        // 'invalid_key_id'
        String authenticationErrorCode = response.header("x-authentication-error");
        if (authenticationErrorCode != null
                && Arrays.asList("invalid_key_id", "expired_signing_key")
                        .contains(authenticationErrorCode)) {
            retrofit2.Response<SigningKey> newKeyResponse = issueNewKey();
            if (newKeyResponse.isSuccessful()) {
                SigningKey newKey = newKeyResponse.body();
                keychain.appendKey(newKey);
                signedRequest = requestSigner.signRequestByKey(chain.request(), newKey);
                return chain.proceed(signedRequest);
            } else {
                // return response of a request of signing key to infer http error
                return newKeyResponse.raw();
            }
        }
        return response;
    }

    private retrofit2.Response<SigningKey> issueNewKey() throws IOException {
        return signingService.issueKey().execute();
    }
}
