package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.level.block.BaseEntityBlock;

/** Entity-block base used on every version. On 26.x this is a plain BaseEntityBlock. */
public abstract class CompatEntityBlock extends BaseEntityBlock {
    protected CompatEntityBlock(Properties properties) {
        super(properties);
    }
}
