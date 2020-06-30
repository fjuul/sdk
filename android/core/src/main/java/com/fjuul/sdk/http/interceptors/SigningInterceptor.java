package com.fjuul.sdk.http.interceptors;

import java.io.IOException;
import java.util.Optional;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.utils.RequestSigner;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SigningInterceptor implements Interceptor {
    private SigningKeychain keychain;
    private RequestSigner requestSigner;

    public SigningInterceptor(SigningKeychain keychain, RequestSigner requestSigner) {
        this.keychain = keychain;
        this.requestSigner = requestSigner;
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
        Request signedRequest = requestSigner.signRequestByKey(chain.request(), key);
        return chain.proceed(signedRequest);
    }
}
