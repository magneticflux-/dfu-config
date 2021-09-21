package com.skaggsm.dfu_config.impl;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * A getter that uses a {@link Method} and handles reflective failures.
 *
 * @param <T> the type of object to retrieve from
 */
public record MethodGetter<T>(MethodHandle handle) implements Function<T, Object> {
    @Override
    public Object apply(T t) {
        try {
            return handle.invoke(t);
        } catch (Throwable e) {
            throw new IllegalStateException("Codec builder method invoke failed!", e);
        }
    }
}
