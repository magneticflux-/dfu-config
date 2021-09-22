package com.skaggsm.dfu_config.impl;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.impl.clazz.ClassBuilder;
import com.skaggsm.dfu_config.impl.iface.ProxyInterfaceBuilder;
import com.skaggsm.dfu_config.impl.record.RecordBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for building objects from their constituent fields.
 *
 * @param <T> the type of the built objects
 */
public interface ObjectBuilder<T> {
    @NotNull
    static <T> ObjectBuilder<T> buildObjectBuilder(Class<T> type) {
        try {
            if (type.isInterface())
                return new ProxyInterfaceBuilder<>(type);
            else if (type.isRecord())
                return new RecordBuilder<>(type);
            else
                return new ClassBuilder<>(type);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalStateException("Failed to create ObjectBuilder for class \"%s\"!".formatted(type), e);
        }
    }

    @NotNull
    T build(Object[] fields);

    @NotNull
    RecordCodecBuilder<T, ?>[] getRecordFields();
}
