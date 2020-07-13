package com.fjuul.sdk.http.interceptors;

import java.io.IOException;
import java.util.Optional;

import com.fjuul.sdk.entities.SigningKey;
import com.fjuul.sdk.entities.SigningKeychain;
import com.fjuul.sdk.http.responses.UnauthorizedErrorResponseBody;
import com.fjuul.sdk.http.services.ISigningService;
import com.fjuul.sdk.http.utils.RequestSigner;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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
            SigningKey newKey = issueNewKey();
            if (newKey != null) {
                keychain.appendKey(newKey);
                signingKey = newKey;
            } else {
                // TODO: specify an error cause (invalid credentials ?)
                throw new IOException("Couldn't retrieve a signing key");
            }
        } else {
            signingKey = keyOptional.get();
        }

        Request signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
        Response response = chain.proceed(signedRequest);
        if (response.isSuccessful()) {
            return response;
        }

        // NOTE: try to retrieve a signing key once if error_code is `expired_signing_key`
        if (response.code() == 401) {
            // TODO: lookup error code by header
            ResponseBody responseBody = response.body();
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<UnauthorizedErrorResponseBody> bodyJsonAdapter =
                    moshi.adapter(UnauthorizedErrorResponseBody.class).nullSafe();
            UnauthorizedErrorResponseBody errorResponseBody =
                    bodyJsonAdapter.fromJson(responseBody.source());
            if (errorResponseBody != null
                    && errorResponseBody.getErrorCode()
                            == UnauthorizedErrorResponseBody.ErrorCode.expired_signing_key) {
                SigningKey newKey = issueNewKey();
                if (newKey != null) {
                    keychain.appendKey(newKey);
                    signingKey = newKey;
                    signedRequest = requestSigner.signRequestByKey(chain.request(), signingKey);
                    return chain.proceed(signedRequest);
                } else {
                    // TODO: specify an error cause (invalid credentials ?)
                    throw new IOException("Couldn't refresh a signing key");
                }
            }
        }
        return response;
    }

    private SigningKey issueNewKey() throws IOException {
        retrofit2.Response<SigningKey> response = signingService.issueKey().execute();
        if (response.isSuccessful()) {
            return response.body();
        } else {
            return null;
        }
    }
}
