package com.workshopcraft.workshopcore.compat;

import com.workshopcraft.workshopcore.compat.component.DataComponentType;

/** Stand-in for the 26.x DataComponentGetter. */
public interface DataComponentGetter {
    <T> T get(DataComponentType<T> type);

    default <T> T getOrDefault(DataComponentType<T> type, T def) {
        final T v = get(type);
        return v != null ? v : def;
    }

    default boolean has(DataComponentType<?> type) {
        return get(type) != null;
    }
}
