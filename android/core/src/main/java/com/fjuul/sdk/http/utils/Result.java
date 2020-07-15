package com.fjuul.sdk.http.utils;

public final class Result<T, E extends Error> {
    public static <T, E extends Error> Result<T, E> value(T value) {
        return new Result<T, E>(value, null);
    }

    public static <T, E extends Error> Result<T, E> error(E error) {
        return new Result<>(null, error);
    }

    private final T value;
    private final E error;

    private Result(T value, E error) {
        this.value = value;
        this.error = error;
    }

    public E getError() {
        return error;
    }

    public T getValue() {
        return value;
    }

    public boolean isError() {
        return error != null;
    }
}
