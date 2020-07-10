package com.fjuul.sdk.http.utils;

public final class Result<T, E extends Error> {
    public static <T,E extends Error> Result<T,E> success(T success) {
        return new Result<T, E>(success, null);
    }
    public static <T,E extends Error> Result<T,E> error(E error) {
        return new Result<>(null, error);
    }
    private final T success;
    private final E error;

    private Result(T success, E error) {
        this.success = success;
        this.error = error;
    }

    public E error() {
        return error;
    }

    public boolean isError() {
        return error != null;
    }
}
