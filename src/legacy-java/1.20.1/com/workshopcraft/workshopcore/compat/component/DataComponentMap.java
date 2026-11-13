package com.workshopcraft.workshopcore.compat.component;

import java.util.IdentityHashMap;
import java.util.Map;

/** Stand-in for DataComponentMap; only the builder is used. */
public final class DataComponentMap {
    public static final class Builder {
        final Map<DataComponentType<?>, Object> values = new IdentityHashMap<>();

        public <T> Builder set(DataComponentType<T> type, T value) {
            values.put(type, value);
            return this;
        }

        public Map<DataComponentType<?>, Object> values() {
            return values;
        }
    }
}
