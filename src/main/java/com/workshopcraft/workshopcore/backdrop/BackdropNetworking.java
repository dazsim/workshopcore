package com.workshopcraft.workshopcore.backdrop;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.BiConsumer;

/** Loader hooks for sending backdrop packets. */
public final class BackdropNetworking {
    static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    static BiConsumer<ServerLevel, CustomPacketPayload> sendToLevel = (level, payload) -> {};

    private BackdropNetworking() {
    }

    public static void setHooks(BiConsumer<ServerPlayer, CustomPacketPayload> toPlayer, BiConsumer<ServerLevel, CustomPacketPayload> toLevel) {
        sendToPlayer = toPlayer;
        sendToLevel = toLevel;
    }

    public static void sendAllTo(ServerPlayer player) {
        sendToPlayer.accept(player, new BackdropDefsPacket(Backdrops.copyDefs()));
        Backdrops.sendAssignments(player);
    }

    public static void sendDefsTo(ServerPlayer player) {
        sendToPlayer.accept(player, new BackdropDefsPacket(Backdrops.copyDefs()));
    }

    /** Instant assignment (login, reload, dimension change). */
    public static void sendAssignment(ServerPlayer player, ResourceKey<Level> dimension, Optional<Identifier> id) {
        sendAssignment(player, dimension, id, 0);
    }

    public static void sendAssignment(ServerPlayer player, ResourceKey<Level> dimension, Optional<Identifier> id, int transitionTicks) {
        sendToPlayer.accept(player, new BackdropSyncPacket(dimension, id, transitionTicks));
    }

    public static void syncLevel(ServerLevel level) {
        syncLevel(level, 0);
    }

    public static void syncLevel(ServerLevel level, int transitionTicks) {
        sendToLevel.accept(level, new BackdropSyncPacket(level.dimension(), Backdrops.get(level), transitionTicks));
    }

    public static void forgetDimension(MinecraftServer server, ResourceKey<Level> dimension) {
        BackdropSyncPacket packet = new BackdropSyncPacket(dimension, Optional.empty(), 0);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer.accept(player, packet);
        }
    }
}
