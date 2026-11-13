package com.workshopcraft.workshopcore.backdrop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.compat.CodecSavedData;
import com.workshopcraft.workshopcore.compat.SavedDataType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;

import java.util.Optional;

/** Per-level saved backdrop id. Missing file means not yet initialized, so a data default can still apply. */
public final class BackdropData extends CodecSavedData {
    public static final Codec<BackdropData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id").forGetter(BackdropData::id),
            Codec.BOOL.fieldOf("initialized").forGetter(BackdropData::initialized)
    ).apply(instance, BackdropData::new));

    public static final SavedDataType<BackdropData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "backdrop"),
            () -> new BackdropData(Optional.empty(), false),
            CODEC,
            DataFixTypes.LEVEL
    );

    private Optional<Identifier> id;
    private boolean initialized;

    public BackdropData(Optional<Identifier> id, boolean initialized) {
        this.id = id;
        this.initialized = initialized;
    }

    public Optional<Identifier> id() {
        return id;
    }

    public boolean initialized() {
        return initialized;
    }

    void assign(Optional<Identifier> id) {
        this.id = id;
        this.initialized = true;
        setDirty();
    }
}
