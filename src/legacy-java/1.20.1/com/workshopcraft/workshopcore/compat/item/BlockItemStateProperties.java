package com.workshopcraft.workshopcore.compat.item;

import net.minecraft.nbt.CompoundTag;

/** 1.20.1 stand-in for the block-state item component, backed by the BlockStateTag compound. */
public record BlockItemStateProperties(CompoundTag tag) {
}
