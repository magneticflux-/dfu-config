package com.skaggsm.dfu_config.impl;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skaggsm.dfu_config.impl.iface.ProxyInterfaceBuilder;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

/**
 * An interface for building objects from their constituent fields.
 *
 * @param <T> the type of the built objects
 */
public interface ObjectBuilder<T> {
    @NotNull
    static <T> ObjectBuilder<T> buildObjectBuilder(Class<T> type) {
        if (type.isInterface()) {
            try {
                return new ProxyInterfaceBuilder<>(type);
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new IllegalStateException("Failed to create ProxyInterfaceBuilder!", e);
            }
        } else if (type.isRecord()) {
            throw new NotImplementedException("Record types not implemented");
        } else
            throw new NotImplementedException("Classes not implemented");
    }

    @NotNull
    T build(Object[] fields);

    @NotNull
    RecordCodecBuilder<T, ?>[] getRecordFields();
}
