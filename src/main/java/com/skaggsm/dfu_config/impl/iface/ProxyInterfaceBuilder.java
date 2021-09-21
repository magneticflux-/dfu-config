package com.skaggsm.dfu_config.impl.iface;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.ConfigBuilder;
import com.skaggsm.dfu_config.impl.MethodGetter;
import com.skaggsm.dfu_config.impl.ObjectBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

public class ProxyInterfaceBuilder<T> implements ObjectBuilder<T> {
    private final Class<T> iface;
    private final RecordCodecBuilder<T, ?>[] fields;
    private final Object2IntMap<String> namesToIndexes = new Object2IntOpenHashMap<>();
    private final MethodHandles.Lookup lookup;

    public ProxyInterfaceBuilder(Class<T> iface) throws IllegalAccessException, NoSuchMethodException {
        this.iface = iface;
        lookup = MethodHandles.privateLookupIn(iface, MethodHandles.lookup());

        var methods = Arrays.stream(iface.getDeclaredMethods())
                .filter(ProxyInterfaceBuilder::validMethod)
                .toArray(Method[]::new);
        int methodsLength = methods.length;

        //noinspection unchecked
        fields = (RecordCodecBuilder<T, ?>[]) new RecordCodecBuilder<?, ?>[methodsLength];
        for (int i = 0; i < methodsLength; i++) {
            Method method = methods[i];
            var name = method.getName();
            var handle = lookup.unreflect(method);
            namesToIndexes.put(name, i);
            fields[i] = ConfigBuilder.buildCodec(method.getGenericReturnType())
                    .fieldOf(name)
                    .forGetter(new MethodGetter<>(handle));
        }
    }

    private static boolean validMethod(Method method) {
        return !method.isDefault() && !method.isSynthetic() && !method.isBridge();
    }

    @Override
    public String toString() {
        return "ProxyInterfaceBuilder[%s]".formatted(iface.getSimpleName());
    }

    @Override
    @NotNull
    public T build(Object[] fields) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class[]{iface},
                new MethodInvocationHandler<>(iface, lookup, fields, namesToIndexes)
        );
    }

    @Override
    public RecordCodecBuilder<T, ?>[] getRecordFields() {
        return fields;
    }

}
