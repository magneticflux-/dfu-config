package com.skaggsm.dfu_config;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.impl.ObjectBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ConfigBuilder {
    private final static Map<Type, Codec<?>> CODEC_CACHE = new Object2ObjectOpenHashMap<>();

    @NotNull
    public static <T> Codec<T> buildCodec(Class<T> type) {
        return buildCodec((Type) type);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T> Codec<T> buildCodec(Type type) {
        var cached = (Codec<T>) CODEC_CACHE.get(type);
        if (cached != null)
            return cached;

        Codec<T> builtin = builtinCodec(type);
        if (builtin != null) {
            CODEC_CACHE.put(type, builtin);
            return builtin;
        }

        @SuppressWarnings("unchecked")
        var objectBuilder = ObjectBuilder.buildObjectBuilder((Class<T>) type);

        var fields = objectBuilder.getRecordFields();

        var codec = RecordCodecBuilder.<T>create(inst -> {
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

                return (App<RecordCodecBuilder.Mu<T>, T>) apply.invoke(product, inst, builderFunction);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to create Codec for \"%s\"!".formatted(type), e);
            }
        });
        CODEC_CACHE.put(type, codec);
        return codec;
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
        } else if (clazz == Identifier.class) {
            return (Codec<T>) Identifier.CODEC;
        } else if (clazz == Item.class) {
            return (Codec<T>) Registry.ITEM;
        } else if (clazz == Block.class) {
            return (Codec<T>) Registry.BLOCK;
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
