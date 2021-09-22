package com.skaggsm.dfu_config.impl.clazz;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.ConfigBuilder;
import com.skaggsm.dfu_config.impl.MethodGetter;
import com.skaggsm.dfu_config.impl.ObjectBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class ClassBuilder<T> implements ObjectBuilder<T> {
    private final RecordCodecBuilder<T, ?>[] fields;
    private final MethodHandle constructor;

    public ClassBuilder(Class<T> clazz) throws IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        var fields = Arrays.stream(clazz.getDeclaredFields()).filter(ClassBuilder::testField).toArray(Field[]::new);
        //noinspection unchecked
        this.fields = (RecordCodecBuilder<T, ?>[]) new RecordCodecBuilder<?, ?>[fields.length];
        for (int i = 0, componentsLength = fields.length; i < componentsLength; i++) {
            var field = fields[i];
            this.fields[i] = ConfigBuilder.buildCodec(field.getGenericType())
                    .fieldOf(field.getName())
                    .forGetter(new MethodGetter<>(lookup.unreflectGetter(field)));
        }

        constructor = lookup.unreflectConstructor(clazz.getDeclaredConstructors()[0]);
    }

    private static boolean testField(Field f) {
        var mod = f.getModifiers();
        if (Modifier.isStatic(mod))
            return false;
        else if (Modifier.isTransient(mod))
            return false;
        else if (!Modifier.isFinal(mod))
            throw new IllegalArgumentException("All candidate config fields must be final!");
        else
            return Modifier.isPublic(mod);
    }

    @Override
    public @NotNull T build(Object[] fields) {
        try {
            //noinspection unchecked
            return (T) constructor.invokeWithArguments(fields);
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to construct record!", e);
        }
    }

    @Override
    public @NotNull RecordCodecBuilder<T, ?>[] getRecordFields() {
        return this.fields;
    }
}
