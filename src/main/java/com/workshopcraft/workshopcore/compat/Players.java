package com.workshopcraft.workshopcore.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class Players {
    private Players() {
    }

    public static void overlay(Player player, Component message) {
        //? if >=26.1 {
        player.sendOverlayMessage(message);
        //?} else {
        /*player.displayClientMessage(message, true);
        *///?}
    }

    public static double reach(Player player) {
        //? if >=1.20.5 {
        return player.blockInteractionRange();
        //?} else {
        /*return player.isCreative() ? 5.0 : 4.5;
        *///?}
    }
}
