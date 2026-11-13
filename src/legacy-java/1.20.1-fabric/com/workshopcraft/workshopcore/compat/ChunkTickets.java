package com.workshopcraft.workshopcore.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;

/** Chunk loader tickets that load and tick like a forced chunk, and are not a vanilla /forceload ticket. */
public final class ChunkTickets {
    private static final TicketType<ChunkPos> TYPE = TicketType.create("workshopcore:chunk_loader", Comparator.comparingLong(ChunkPos::toLong));

    private ChunkTickets() {}

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos);
    }
}
