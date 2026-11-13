package com.workshopcraft.workshopcore.compat;

public interface ComponentHolder {
    void applyComponentsFrom(DataComponentGetter components);

    /** Item NBT compound key for implicit components, e.g. "SimpleMachines". Null skips them. */
    default String componentStorageKey() {
        return null;
    }
}
