package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;

import java.util.Optional;

/** Version-stable reader for block entity and similar NBT, wrapping the 26.x ValueInput. */
public final class ValueInput {
    private final net.minecraft.world.level.storage.ValueInput in;

    private ValueInput(net.minecraft.world.level.storage.ValueInput in) {
        this.in = in;
    }

    public static ValueInput of(net.minecraft.world.level.storage.ValueInput in) {
        return new ValueInput(in);
    }

    public net.minecraft.world.level.storage.ValueInput vanilla() {
        return in;
    }

    public int getIntOr(String key, int def) {
        return in.getIntOr(key, def);
    }

    public long getLongOr(String key, long def) {
        return in.getLongOr(key, def);
    }

    public float getFloatOr(String key, float def) {
        return in.getFloatOr(key, def);
    }

    public boolean getBooleanOr(String key, boolean def) {
        return in.getBooleanOr(key, def);
    }

    public String getStringOr(String key, String def) {
        return in.getStringOr(key, def);
    }

    public Optional<Long> getLong(String key) {
        return in.getLong(key);
    }

    public Optional<Integer> getInt(String key) {
        return in.getInt(key);
    }

    public Optional<String> getString(String key) {
        return in.getString(key);
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        return in.read(key, codec);
    }
}
