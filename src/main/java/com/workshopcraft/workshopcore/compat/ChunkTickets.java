package com.workshopcraft.workshopcore.compat;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.function.BiConsumer;

/**
 * Loading and ticking chunk tickets that are not written to the vanilla ticket file.
 * On 26.x, call {@link #register} from your loader init, or rely on Workshop Core's own init.
 */
public final class ChunkTickets {
    public static final TicketType TYPE = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    public static final Identifier ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "chunk_loader");

    private ChunkTickets() {
    }

    public static void register(BiConsumer<Identifier, TicketType> registrar) {
        registrar.accept(ID, TYPE);
    }

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addTicketWithRadius(TYPE, pos, radius);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeTicketWithRadius(TYPE, pos, radius);
    }
}
