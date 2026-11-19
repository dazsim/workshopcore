package com.workshopcraft.workshopcore.machine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

/** What powers a machine. Built in: {@link Drives#CRANK}, {@link Drives#FUEL}, {@link Drives#ENERGY}. */
public interface Drive {
    /** Called on each server tick the machine has work. Return true to advance progress by one tick. */
    boolean run(WorkBlockEntity machine, ServerLevel level, int energyCost);

    /** Empty hand use on the machine. Return true if the drive handled it. */
    default boolean crank(WorkBlockEntity machine, Player player) {
        return false;
    }

    /** Fill level 0..1000 for menus. */
    default int level(WorkBlockEntity machine) {
        return 0;
    }

    /** True if this drive takes its power from the machine's EnergyBuffer. */
    default boolean usesEnergy() {
        return false;
    }
}
