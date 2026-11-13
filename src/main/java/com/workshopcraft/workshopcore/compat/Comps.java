package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/** Item component access. On 26.x and 1.21.1 this is a thin wrapper around the vanilla component methods. */
public final class Comps {
    private Comps() {
    }

    public static <T> T get(ItemStack stack, DataComponentType<T> type) {
        return stack.get(type);
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> type, T def) {
        return stack.getOrDefault(type, def);
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        return stack.has(type);
    }

    public static <T> T set(ItemStack stack, DataComponentType<T> type, T value) {
        return stack.set(type, value);
    }

    public static <T> T remove(ItemStack stack, DataComponentType<T> type) {
        return stack.remove(type);
    }

    public static <T> T get(ItemStack stack, String storageKey, DataComponentType<T> type) {
        return get(stack, type);
    }

    public static <T> T getOrDefault(ItemStack stack, String storageKey, DataComponentType<T> type, T def) {
        return getOrDefault(stack, type, def);
    }

    public static boolean has(ItemStack stack, String storageKey, DataComponentType<?> type) {
        return has(stack, type);
    }

    public static <T> T set(ItemStack stack, String storageKey, DataComponentType<T> type, T value) {
        return set(stack, type, value);
    }

    public static <T> T remove(ItemStack stack, String storageKey, DataComponentType<T> type) {
        return remove(stack, type);
    }
}
