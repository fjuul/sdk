package com.fjuul.sdk.http.utils;

import android.annotation.SuppressLint;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class ApiCallAdapterFactory extends CallAdapter.Factory {
    @NonNull
    public static ApiCallAdapterFactory create(@NonNull IApiResponseTransformer responseTransformer) {
        return new ApiCallAdapterFactory(responseTransformer);
    }

    @NonNull
    public static ApiCallAdapterFactory create() {
        return new ApiCallAdapterFactory(new DefaultApiResponseTransformer());
    }

    private IApiResponseTransformer responseTransformer;

    private ApiCallAdapterFactory(IApiResponseTransformer responseTransformer) {
        this.responseTransformer = responseTransformer;
    }

    @SuppressLint("UnknownNullness")
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(returnType) != ApiCall.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                "ApiCall return type must be parameterized" + " as ApiCall<Foo> or ApiCall<? extends Foo>");
        }
        Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

        return new ApiCallAdapter<>(innerType, responseTransformer);
    }

    private static final class ApiCallAdapter<R> implements CallAdapter<R, ApiCall<R>> {
        private final Type responseType;
        private final IApiResponseTransformer<R> responseTransformer;

        ApiCallAdapter(Type responseType, IApiResponseTransformer<R> responseTransformer) {
            this.responseType = responseType;
            this.responseTransformer = responseTransformer;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public ApiCall<R> adapt(final Call<R> call) {
            return new ApiCall<>(call, responseTransformer);
        }
    }
}
