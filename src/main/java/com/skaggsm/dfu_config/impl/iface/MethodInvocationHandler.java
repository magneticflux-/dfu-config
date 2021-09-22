package com.skaggsm.dfu_config.impl.iface;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * An {@link InvocationHandler} that returns constant values for parameterless methods.
 */
record MethodInvocationHandler<T>(
        Class<T> iface,
        MethodHandles.Lookup lookup,
        Object[] fields,
        Object2IntMap<String> namesToIndexes
) implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] suppliedArgs) {
        switch (method.getName()) {
            case "toString" -> {
                return "Fake %s".formatted(iface);
            }
            case "equals" -> throw new UnsupportedOperationException("\"equals\" not implemented yet!");
            case "hashCode" -> throw new UnsupportedOperationException("\"hashCode\" not implemented yet!");
            default -> {
                if (suppliedArgs == null || suppliedArgs.length == 0) {
                    var index = namesToIndexes.getOrDefault(method.getName(), -1);
                    if (index >= 0)
                        return fields[index];
                }
                if (method.isDefault()) {
                    try {
                        return lookup.unreflectSpecial(method, method.getDeclaringClass()).bindTo(proxy).invokeWithArguments(suppliedArgs);
                    } catch (Throwable e) {
                        throw new IllegalStateException("Default method call failed!", e);
                    }
                }
                throw new UnsupportedOperationException("Method %s not implemented!".formatted(method));
            }
        }
    }
}
