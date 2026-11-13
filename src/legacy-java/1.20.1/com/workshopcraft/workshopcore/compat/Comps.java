package com.workshopcraft.workshopcore.compat;

import com.workshopcraft.workshopcore.compat.component.DataComponentMap;
import com.workshopcraft.workshopcore.compat.component.DataComponentType;
import com.workshopcraft.workshopcore.compat.component.DataComponents;
import com.workshopcraft.workshopcore.compat.item.BlockItemStateProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Item component access for 1.20.1, stored under a compound key you pass (for example "SimpleMachines")
 * so existing item NBT stays valid.
 */
public final class Comps {
    private Comps() {
    }

    public static <T> T get(ItemStack stack, DataComponentType<T> type) {
        return get(stack, type.storage(), type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ItemStack stack, String storageKey, DataComponentType<T> type) {
        if (type == DataComponents.DYE) return stack.getItem() instanceof DyeItem d ? (T) d.getDyeColor() : null;
        final CompoundTag tag = stack.getTag();
        if (type == DataComponents.BLOCK_STATE) {
            return tag != null && tag.contains("BlockStateTag", Tag.TAG_COMPOUND) ? (T) new BlockItemStateProperties(tag.getCompound("BlockStateTag")) : null;
        }
        if (storageKey == null) return null;
        return read(tag == null ? null : tag.getCompound(storageKey), type);
    }

    public static <T> T read(CompoundTag data, DataComponentType<T> type) {
        if (data == null || type.codec() == null || type.name() == null || !data.contains(type.name())) return null;
        return type.codec().parse(NbtOps.INSTANCE, data.get(type.name())).result().orElse(null);
    }

    public static <T> T getOrDefault(ItemStack stack, DataComponentType<T> type, T def) {
        final T v = get(stack, type);
        return v != null ? v : def;
    }

    public static <T> T getOrDefault(ItemStack stack, String storageKey, DataComponentType<T> type, T def) {
        final T v = get(stack, storageKey, type);
        return v != null ? v : def;
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        return get(stack, type) != null;
    }

    public static boolean has(ItemStack stack, String storageKey, DataComponentType<?> type) {
        return get(stack, storageKey, type) != null;
    }

    public static <T> T set(ItemStack stack, DataComponentType<T> type, T value) {
        return set(stack, type.storage(), type, value);
    }

    public static <T> T set(ItemStack stack, String storageKey, DataComponentType<T> type, T value) {
        final T old = get(stack, storageKey, type);
        if (value == null) {
            remove(stack, storageKey, type);
            return old;
        }
        if (type == DataComponents.BLOCK_STATE) {
            stack.getOrCreateTag().put("BlockStateTag", ((BlockItemStateProperties) value).tag().copy());
            return old;
        }
        if (type.codec() == null || storageKey == null) return old;
        final CompoundTag data = stack.getOrCreateTagElement(storageKey);
        type.codec().encodeStart(NbtOps.INSTANCE, value).result().ifPresent(t -> data.put(type.name(), t));
        return old;
    }

    public static <T> T remove(ItemStack stack, DataComponentType<T> type) {
        return remove(stack, type.storage(), type);
    }

    public static <T> T remove(ItemStack stack, String storageKey, DataComponentType<T> type) {
        final T old = get(stack, storageKey, type);
        if (type == DataComponents.BLOCK_STATE) {
            stack.removeTagKey("BlockStateTag");
            return old;
        }
        final CompoundTag tag = stack.getTag();
        if (storageKey != null && tag != null && tag.contains(storageKey, Tag.TAG_COMPOUND)) {
            final CompoundTag data = tag.getCompound(storageKey);
            if (type.name() != null) data.remove(type.name());
            if (data.isEmpty()) stack.removeTagKey(storageKey);
        }
        return old;
    }

    public static void write(CompoundTag data, DataComponentMap.Builder components) {
        for (Map.Entry<DataComponentType<?>, Object> e : components.values().entrySet()) put(data, e.getKey(), e.getValue());
    }

    @SuppressWarnings("unchecked")
    private static <T> void put(CompoundTag data, DataComponentType<T> type, Object value) {
        if (type.codec() != null && type.name() != null) {
            type.codec().encodeStart(NbtOps.INSTANCE, (T) value).result().ifPresent(t -> data.put(type.name(), t));
        }
    }

    public static DataComponentGetter getter(ItemStack stack) {
        return getter(stack, null);
    }

    public static DataComponentGetter getter(ItemStack stack, String storageKey) {
        return new DataComponentGetter() {
            public <T> T get(DataComponentType<T> type) {
                return storageKey != null ? Comps.get(stack, storageKey, type) : Comps.get(stack, type);
            }
        };
    }

    public static DataComponentGetter getterFrom(CompoundTag data) {
        return new DataComponentGetter() {
            public <T> T get(DataComponentType<T> type) {
                return Comps.read(data, type);
            }
        };
    }
}
