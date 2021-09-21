package com.skaggsm.dfu_config.impl.record;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.ConfigBuilder;
import com.skaggsm.dfu_config.impl.MethodGetter;
import com.skaggsm.dfu_config.impl.ObjectBuilder;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class RecordBuilder<T> implements ObjectBuilder<T> {
    private final RecordCodecBuilder<T, ?>[] fields;
    private final MethodHandle constructor;

    public RecordBuilder(Class<T> record) throws IllegalAccessException {
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(record, MethodHandles.lookup());
        var components = record.getRecordComponents();
        //noinspection unchecked
        fields = (RecordCodecBuilder<T, ?>[]) new RecordCodecBuilder<?, ?>[components.length];
        for (int i = 0, componentsLength = components.length; i < componentsLength; i++) {
            var component = components[i];
            var accessor = component.getAccessor();
            var handle = lookup.unreflect(accessor);
            fields[i] = ConfigBuilder.buildCodec(accessor.getGenericReturnType())
                    .fieldOf(component.getName())
                    .forGetter(new MethodGetter<>(handle));
        }

        constructor = lookup.unreflectConstructor(record.getDeclaredConstructors()[0]);
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
