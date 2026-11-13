package com.workshopcraft.workshopcore;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** ResourceKey id accessor that tracks the 26.x rename of location() to identifier(). */
public final class Ids {
    private Ids() {
    }

    public static Identifier of(ResourceKey<?> key) {
        //? if >=26.1 {
        return key.identifier();
        //?} else {
        /*return key.location();*/
        //?}
    }
}
