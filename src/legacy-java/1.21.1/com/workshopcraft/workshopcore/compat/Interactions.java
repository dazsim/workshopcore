package com.workshopcraft.workshopcore.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

/** Maps 26.x style block item-use results onto 1.21.1's ItemInteractionResult. */
public final class Interactions {
    public static final InteractionResult TRY_WITH_EMPTY_HAND = InteractionResult.SUCCESS_NO_ITEM_USED;

    private Interactions() {
    }

    public static ItemInteractionResult item(InteractionResult r) {
        return switch (r) {
            case SUCCESS_NO_ITEM_USED -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case PASS -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            case SUCCESS -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }
}
