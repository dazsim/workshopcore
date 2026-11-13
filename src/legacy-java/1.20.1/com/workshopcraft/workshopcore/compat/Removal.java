package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** 1.20.1 has no preRemoveSideEffects; blocks call this from onRemove. */
public interface Removal {
    void preRemoveSideEffects(BlockPos pos, BlockState state);

    static void onRemove(Level level, BlockPos pos, BlockState state, BlockState newState) {
        if (!level.isClientSide() && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Removal r) {
            r.preRemoveSideEffects(pos, state);
        }
    }
}
