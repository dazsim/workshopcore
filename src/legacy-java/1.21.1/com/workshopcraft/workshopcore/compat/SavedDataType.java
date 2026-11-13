package com.workshopcraft.workshopcore.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.function.Supplier;

/**
 * Stand-in for the 26.x SavedDataType. The storage key is {@code namespace_path} from the id the caller passes,
 * matching the scheme already used by Simple Machines on this version.
 */
public record SavedDataType<T extends CodecSavedData>(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, DataFixTypes fix) {
    public T get(MinecraftServer server) {
        return get(server.overworld());
    }

    public T get(ServerLevel level) {
        final SavedData.Factory<T> factory = new SavedData.Factory<>(() -> attach(constructor.get()),
                (tag, registries) -> attach(codec.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("data"))
                        .result().orElseGet(constructor)), fix);
        return level.getDataStorage().computeIfAbsent(factory, id.getNamespace() + "_" + id.getPath());
    }

    private T attach(T data) {
        data.codec = codec;
        return data;
    }

    static CompoundTag save(CodecSavedData data, CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        @SuppressWarnings("unchecked") final Codec<CodecSavedData> c = (Codec<CodecSavedData>) data.codec;
        c.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), data).result().ifPresent(t -> tag.put("data", t));
        return tag;
    }
}
