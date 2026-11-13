package com.workshopcraft.workshopcore.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** SavedData saved through the codec of the SavedDataType that made it. */
public abstract class CodecSavedData extends SavedData {
    com.mojang.serialization.Codec<?> codec;

    @Override
    public CompoundTag save(CompoundTag tag) {
        return SavedDataType.save(this, tag);
    }
}
