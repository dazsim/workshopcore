package com.workshopcraft.workshopcore.compat;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Loading and ticking chunk tickets that are not written to the vanilla ticket file.
 * On 26.x, register ticket types during loader init.
 *
 * The static {@link #add} / {@link #remove} methods share one type ({@link #ID}).
 * If two mods hold the same chunk and radius with those methods, one remove drops both.
 * Prefer {@link #create(Identifier)} for an isolated type per owner.
 */
public final class ChunkTickets {
    public static final TicketType TYPE = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    public static final Identifier ID = Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "chunk_loader");

    private static final ConcurrentHashMap<Identifier, Owner> OWNERS = new ConcurrentHashMap<>();

    private ChunkTickets() {
    }

    public static void register(BiConsumer<Identifier, TicketType> registrar) {
        registrar.accept(ID, TYPE);
        for (Owner owner : OWNERS.values()) {
            owner.register(registrar);
        }
    }

    public static void add(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().addTicketWithRadius(TYPE, pos, radius);
    }

    public static void remove(ServerLevel level, ChunkPos pos, int radius) {
        level.getChunkSource().removeTicketWithRadius(TYPE, pos, radius);
    }

    /**
     * Isolated ticket type for this owner id. Two owners can hold the same chunk and
     * radius independently. On 26.x, register the type during your loader init with
     * {@link Owner#register} (Workshop Core's own init also registers owners already
     * created at that point).
     */
    public static Owner create(Identifier id) {
        return OWNERS.computeIfAbsent(id, Owner::new);
    }

    public static final class Owner {
        private final Identifier id;
        private final TicketType type;

        private Owner(Identifier id) {
            this.id = id;
            this.type = new TicketType(TicketType.NO_TIMEOUT,
                    TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
        }

        public Identifier id() {
            return id;
        }

        public TicketType type() {
            return type;
        }

        public void register(BiConsumer<Identifier, TicketType> registrar) {
            registrar.accept(id, type);
        }

        public void add(ServerLevel level, ChunkPos pos, int radius) {
            level.getChunkSource().addTicketWithRadius(type, pos, radius);
        }

        public void remove(ServerLevel level, ChunkPos pos, int radius) {
            level.getChunkSource().removeTicketWithRadius(type, pos, radius);
        }
    }
}
