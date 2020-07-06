package com.fjuul.sdk.http.interceptors;

import java.io.IOException;
import java.util.Optional;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.utils.RequestSigner;

import io.reactivex.Maybe;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class SigningInterceptor implements Interceptor {
    private SigningKeychain keychain;
    private RequestSigner requestSigner;
    private ISigningService signingService;

    public SigningInterceptor(SigningKeychain keychain, RequestSigner requestSigner, ISigningService signingService) {
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
            SigningKey newKey = issueNewKey();
            if (newKey != null) {
                 keychain.appendKey(newKey);
                 signingKey = newKey;
            } else {
                throw new IOException("Couldn't retrieve a signing key");
            }
        } else {
            signingKey = keyOptional.get();
        }

        Request signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
        // TODO: handle case when current valid signing key was rejected by the back-end side
        return chain.proceed(signedRequest);
    }

    private SigningKey issueNewKey() throws IOException {
        SigningKey newKey = signingService.issueKey()
            .firstOrError()
            .flatMapMaybe(signingKeyResult -> {
                if (signingKeyResult.isError()) {
                    return Maybe.error(signingKeyResult.error());
                }

                retrofit2.Response<SigningKey> signingKeyResponse = signingKeyResult.response();
                if (signingKeyResponse.isSuccessful()) {
                    return Maybe.just(signingKeyResponse.body());
                } else {
                    return Maybe.empty();
                }
            }).blockingGet();
        return newKey;
    }
}
