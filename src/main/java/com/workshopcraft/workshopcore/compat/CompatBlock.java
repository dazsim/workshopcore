package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.level.block.Block;

/** Block base used on every version. On 26.x this is a plain Block. */
public class CompatBlock extends Block {
    protected CompatBlock(Properties properties) {
        super(properties);
    }
}
