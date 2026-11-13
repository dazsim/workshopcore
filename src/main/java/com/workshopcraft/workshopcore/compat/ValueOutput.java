package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;

/** Version-stable writer for block entity and similar NBT, wrapping the 26.x ValueOutput. */
public final class ValueOutput {
    private final net.minecraft.world.level.storage.ValueOutput out;

    private ValueOutput(net.minecraft.world.level.storage.ValueOutput out) {
        this.out = out;
    }

    public static ValueOutput of(net.minecraft.world.level.storage.ValueOutput out) {
        return new ValueOutput(out);
    }

    public net.minecraft.world.level.storage.ValueOutput vanilla() {
        return out;
    }

    public void putInt(String key, int v) {
        out.putInt(key, v);
    }

    public void putLong(String key, long v) {
        out.putLong(key, v);
    }

    public void putFloat(String key, float v) {
        out.putFloat(key, v);
    }

    public void putBoolean(String key, boolean v) {
        out.putBoolean(key, v);
    }

    public void putString(String key, String v) {
        out.putString(key, v);
    }

    public void discard(String key) {
        out.discard(key);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        out.store(key, codec, value);
    }

    public <T> void storeNullable(String key, Codec<T> codec, T value) {
        out.storeNullable(key, codec, value);
    }
}
