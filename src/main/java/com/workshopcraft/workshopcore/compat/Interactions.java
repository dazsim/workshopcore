package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.InteractionResult;

/** Shared block-use results. TRY_WITH_EMPTY_HAND means "try the empty-hand hook next". */
public final class Interactions {
    public static final InteractionResult TRY_WITH_EMPTY_HAND =
            //? if >=26.1 {
            InteractionResult.TRY_WITH_EMPTY_HAND
            //?} else if >=1.20.5 {
            /*InteractionResult.SUCCESS_NO_ITEM_USED
            *///?} else {
            /*InteractionResult.CONSUME_PARTIAL
            *///?}
            ;

    private Interactions() {
    }
}
