package com.workshopcraft.workshopcore;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Identifier helpers that track constructor and ResourceKey accessor changes. */
public final class Ids {
    private Ids() {
    }

    public static Identifier of(String namespace, String path) {
        //? if >=1.20.2 {
        return Identifier.fromNamespaceAndPath(namespace, path);
        //?} else {
        /*return new Identifier(namespace, path);*/
        //?}
    }

    public static Identifier of(ResourceKey<?> key) {
        //? if >=26.1 {
        return key.identifier();
        //?} else {
        /*return key.location();*/
        //?}
    }
}
