package com.workshopcraft.workshopcore.compat.component;

import com.workshopcraft.workshopcore.compat.item.BlockItemStateProperties;
import net.minecraft.world.item.DyeColor;

/** Vanilla components read through {@link com.workshopcraft.workshopcore.compat.Comps} on 1.20.1. */
public final class DataComponents {
    public static final DataComponentType<BlockItemStateProperties> BLOCK_STATE = new DataComponentType<>(null, "BlockStateTag");
    public static final DataComponentType<DyeColor> DYE = new DataComponentType<>(null, "$dye");

    private DataComponents() {
    }
}
