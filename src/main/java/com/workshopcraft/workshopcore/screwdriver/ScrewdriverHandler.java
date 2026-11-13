package com.workshopcraft.workshopcore.screwdriver;

/** Callback for a block the registering mod does not own. */
@FunctionalInterface
public interface ScrewdriverHandler {
    ScrewdriverResult useScrewdriver(ScrewdriverContext ctx);
}
