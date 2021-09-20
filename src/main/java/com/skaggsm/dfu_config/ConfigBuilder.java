package com.skaggsm.dfu_config;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;


public class ConfigBuilder {
    @NotNull
    public static <T> Codec<T> buildCodec(Type type) {
        Codec<T> builtin = builtinCodec(type);
        if (builtin != null)
            return builtin;

        List<RecordCodecBuilder<T, ?>> fields = new ArrayList<>();
        Map<String, Integer> namesToIndexes = new HashMap<>();
        var clazz = (Class<?>) type;
        //if (clazz.isInterface()) {
        Method[] methods = clazz.getMethods();
        for (int i = 0, methodsLength = methods.length; i < methodsLength; i++) {
            Method method = methods[i];
            var name = method.getName();
            namesToIndexes.put(name, i);
            fields.add(buildCodec(method.getGenericReturnType()).fieldOf(name).forGetter(obj -> {
                try {
                    return method.invoke(obj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException("Codec builder method invoke failed!", e);
                }
            }));
        }
        /*} else {
            for (var field : clazz.getFields()) {
                fields.add(buildCodec(field.getGenericType()).fieldOf(field.getName()).forGetter(obj -> {
                    try {
                        return field.get(obj);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Codec builder field get failed!", e);
                    }
                }));
            }
        }*/
        //var constructor = clazz.getConstructors()[0];

        return RecordCodecBuilder.create(inst -> {
            try {
                //noinspection JavaReflectionMemberAccess
                var group = RecordCodecBuilder.Instance.class.getMethod("group", appArray(fields.size()));

                var product = group.invoke(inst, (Object[]) fields.toArray(RecordCodecBuilder<?, ?>[]::new));

                var apply = findApplyMethod(product.getClass());

                var functionType = apply.getParameterTypes()[1];

                var proxy = Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{functionType},
                        (self, method, args) ->
                                switch (method.getName()) {
                                    case "apply" -> Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, (self1, method1, args1) -> {
                                        var index = namesToIndexes.get(method1.getName());
                                        if (index != null)
                                            return args1[index];
                                        else
                                            throw new UnsupportedOperationException("Method %s not implemented!".formatted(method));
                                    });
                                    case "toString" -> "%s builder".formatted(clazz.getSimpleName());
                                    default -> throw new UnsupportedOperationException("Method %s not implemented!".formatted(method));
                                });

                //noinspection unchecked
                return (App<RecordCodecBuilder.Mu<T>, T>) apply.invoke(product, inst, proxy);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to create Codec for object!", e);
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
