package com.workshopcraft.workshopcore.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

/** 1.21.1 BaseEntityBlock with removal side effects and affectNeighborsAfterRemoval. */
public abstract class CompatEntityBlock extends BaseEntityBlock {
    protected CompatEntityBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        Removal.onRemove(level, pos, state, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel s) affectNeighborsAfterRemoval(state, s, pos, movedByPiston);
    }

    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
    }
}
