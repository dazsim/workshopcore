package com.workshopcraft.workshopcore.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk loader tickets that load and tick like a forced chunk, and are not a vanilla /forceload ticket.
 * The static add/remove methods share one type; if two mods hold the same chunk and radius, one remove drops both.
 * Prefer {@link #create(ResourceLocation)} for an isolated type per owner.
 */
public final class ChunkTickets {
    private static final TicketType<ChunkPos> TYPE = TicketType.create("workshopcore:chunk_loader", Comparator.comparingLong(ChunkPos::toLong));
    private static final ConcurrentHashMap<ResourceLocation, Owner> OWNERS = new ConcurrentHashMap<>();

    private ChunkTickets() {}

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addRegionTicket(TYPE, pos, radius, pos);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeRegionTicket(TYPE, pos, radius, pos);
    }

    public static Owner create(ResourceLocation id) {
        return OWNERS.computeIfAbsent(id, Owner::new);
    }

    public static final class Owner {
        private final ResourceLocation id;
        private final TicketType<ChunkPos> type;

        private Owner(ResourceLocation id) {
            this.id = id;
            this.type = TicketType.create(id.toString(), Comparator.comparingLong(ChunkPos::toLong));
        }

        public ResourceLocation id() {
            return id;
        }

        public void add(ServerLevel level, ChunkPos pos, int radius) {
            level.getChunkSource().addRegionTicket(type, pos, radius, pos);
        }

        public void remove(ServerLevel level, ChunkPos pos, int radius) {
            level.getChunkSource().removeRegionTicket(type, pos, radius, pos);
        }
    }
}
