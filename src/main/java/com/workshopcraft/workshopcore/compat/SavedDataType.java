package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;

import java.util.function.Supplier;

/**
 * Codec-backed saved data. The id is used as the storage key unchanged, so existing world files stay valid.
 * Use {@link #get(MinecraftServer)} for overworld / server storage, or {@link #get(ServerLevel)} for a level.
 */
public final class SavedDataType<T extends CodecSavedData> {
    private final Identifier id;
    private final net.minecraft.world.level.saveddata.SavedDataType<T> vanilla;

    public SavedDataType(Identifier id, Supplier<T> constructor, Codec<T> codec, DataFixTypes fix) {
        this.id = id;
        this.vanilla = new net.minecraft.world.level.saveddata.SavedDataType<>(id, constructor, codec, fix);
    }

    public Identifier id() {
        return id;
    }

    public T get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(vanilla);
    }

    public T get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(vanilla);
    }
}
