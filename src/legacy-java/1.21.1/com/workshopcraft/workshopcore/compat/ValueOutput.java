package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/** Stand-in for the 26.x ValueOutput, writing into a CompoundTag. */
public record ValueOutput(CompoundTag tag, HolderLookup.Provider registries) {
    public void putInt(String key, int v) {
        tag.putInt(key, v);
    }

    public void putLong(String key, long v) {
        tag.putLong(key, v);
    }

    public void putFloat(String key, float v) {
        tag.putFloat(key, v);
    }

    public void putBoolean(String key, boolean v) {
        tag.putBoolean(key, v);
    }

    public void putString(String key, String v) {
        tag.putString(key, v);
    }

    public void discard(String key) {
        tag.remove(key);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE), value)
                .result().ifPresent(t -> tag.put(key, t));
    }

    public <T> void storeNullable(String key, Codec<T> codec, T value) {
        if (value != null) store(key, codec, value);
    }
}
