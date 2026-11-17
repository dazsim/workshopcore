package com.workshopcraft.workshopcore.ctm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Decides whether the block at pos joins up with the block at otherPos when seen on the given face. */
@FunctionalInterface
public interface ConnectRule {
    boolean connects(BlockGetter level, BlockPos pos, BlockState self, BlockPos otherPos, BlockState other, Direction face);
}
