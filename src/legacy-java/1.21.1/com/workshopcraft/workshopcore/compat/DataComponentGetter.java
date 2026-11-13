package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.component.DataComponentType;

/** Stand-in used where 26.x passes a DataComponentGetter. */
public interface DataComponentGetter {
    <T> T get(DataComponentType<T> type);

    default <T> T getOrDefault(DataComponentType<T> type, T def) {
        final T v = get(type);
        return v != null ? v : def;
    }
}
