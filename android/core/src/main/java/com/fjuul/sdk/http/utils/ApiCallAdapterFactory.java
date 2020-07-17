package com.fjuul.sdk.http.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class ApiCallAdapterFactory extends CallAdapter.Factory {
    public static ApiCallAdapterFactory create() {
        return new ApiCallAdapterFactory();
    }

    private ApiCallAdapterFactory() {}

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

        return new ApiCallAdapter<>(innerType);
    }

    private static final class ApiCallAdapter<R> implements CallAdapter<R, ApiCall<R>> {
        private final Type responseType;

        ApiCallAdapter(Type responseType) {
            this.responseType = responseType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public ApiCall<R> adapt(final Call<R> call) {
            return new ApiCall<>(call);
        }
    }
}
