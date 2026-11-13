package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.InteractionResult;

/** 1.20.1 has no TRY_WITH_EMPTY_HAND; CONSUME_PARTIAL stands in for it. */
public final class Interactions {
    public static final InteractionResult TRY_WITH_EMPTY_HAND = InteractionResult.CONSUME_PARTIAL;

    private Interactions() {
    }
}
