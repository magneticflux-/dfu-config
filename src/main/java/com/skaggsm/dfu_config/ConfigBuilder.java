package com.skaggsm.dfu_config;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.impl.ObjectBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ConfigBuilder {
    @NotNull
    public static <T> Codec<T> buildCodec(Class<T> type) {
        return buildCodec((Type) type);
    }

    @NotNull
    public static <T> Codec<T> buildCodec(Type type) {
        Codec<T> builtin = builtinCodec(type);
        if (builtin != null)
            return builtin;

        @SuppressWarnings("unchecked")
        var objectBuilder = ObjectBuilder.buildObjectBuilder((Class<T>) type);

        var fields = objectBuilder.getRecordFields();

        return RecordCodecBuilder.create(inst -> {
            try {
                //noinspection JavaReflectionMemberAccess
                var group = RecordCodecBuilder.Instance.class.getMethod("group", appArray(fields.length));

                var product = group.invoke(inst, (Object[]) fields);

                var apply = findApplyMethod(product.getClass());

                var functionType = apply.getParameterTypes()[1];

                var builderFunction = Proxy.newProxyInstance(functionType.getClassLoader(), new Class<?>[]{functionType},
                        (proxy, method, args) ->
                                switch (method.getName()) {
                                    case "apply" -> objectBuilder.build(args);
                                    case "toString" -> objectBuilder.toString();
                                    default -> throw new UnsupportedOperationException("Method \"%s\" not implemented!".formatted(method));
                                });

                //noinspection unchecked
                return (App<RecordCodecBuilder.Mu<T>, T>) apply.invoke(product, inst, builderFunction);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to create Codec for \"%s\"!".formatted(type), e);
            }
        });
    }

    private static Method findApplyMethod(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> method.getName().equals("apply") && !method.getParameterTypes()[1].equals(App.class))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Unable to find \"apply\" method!"));
    }

    private static Class<?>[] appArray(int length) {
        var array = new Class<?>[length];
        Arrays.fill(array, App.class);
        return array;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Codec<T> builtinCodec(Type clazz) {
        if (clazz == boolean.class || clazz == Boolean.class) {
            return (Codec<T>) Codec.BOOL;
        } else if (clazz == byte.class || clazz == Byte.class) {
            return (Codec<T>) Codec.BYTE;
        } else if (clazz == short.class || clazz == Short.class) {
            return (Codec<T>) Codec.SHORT;
        } else if (clazz == int.class || clazz == Integer.class) {
            return (Codec<T>) Codec.INT;
        } else if (clazz == long.class || clazz == Long.class) {
            return (Codec<T>) Codec.LONG;
        } else if (clazz == float.class || clazz == Float.class) {
            return (Codec<T>) Codec.FLOAT;
        } else if (clazz == double.class || clazz == Double.class) {
            return (Codec<T>) Codec.DOUBLE;
        } else if (clazz == String.class) {
            return (Codec<T>) Codec.STRING;
        } else if (clazz instanceof ParameterizedType) {
            var raw = (Class<?>) ((ParameterizedType) clazz).getRawType();
            var typeArgs = ((ParameterizedType) clazz).getActualTypeArguments();

            if (List.class.isAssignableFrom(raw)) {
                return (Codec<T>) Codec.list(buildCodec(typeArgs[0]));
            } else if (Map.class.isAssignableFrom(raw)) {
                return (Codec<T>) Codec.unboundedMap(
                        buildCodec(typeArgs[0]),
                        buildCodec(typeArgs[1])
                );
            }
        }
        return null;
    }
}
