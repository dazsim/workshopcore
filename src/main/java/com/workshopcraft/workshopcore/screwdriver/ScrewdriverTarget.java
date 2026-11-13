package com.workshopcraft.workshopcore.screwdriver;

/** Implemented by a block, or its block entity, to react to a screwdriver. */
public interface ScrewdriverTarget {
    ScrewdriverResult useScrewdriver(ScrewdriverContext ctx);
}
