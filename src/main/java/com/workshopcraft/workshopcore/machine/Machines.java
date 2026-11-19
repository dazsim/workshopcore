package com.workshopcraft.workshopcore.machine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Block entity types whose items and energy the loader should expose to hoppers, pipes and cables. */
public final class Machines {
    private static final List<Supplier<? extends BlockEntityType<? extends WorkBlockEntity>>> TYPES = new CopyOnWriteArrayList<>();

    private Machines() {
    }

    /** Call from mod init, before capabilities are registered. Needed on NeoForge; harmless elsewhere. */
    public static void expose(Supplier<? extends BlockEntityType<? extends WorkBlockEntity>> type) {
        TYPES.add(type);
    }

    public static List<Supplier<? extends BlockEntityType<? extends WorkBlockEntity>>> exposed() {
        return TYPES;
    }
}
