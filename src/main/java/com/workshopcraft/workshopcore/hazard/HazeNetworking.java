package com.workshopcraft.workshopcore.hazard;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public final class HazeNetworking {
    static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};

    private HazeNetworking() {
    }

    public static void setHook(BiConsumer<ServerPlayer, CustomPacketPayload> toPlayer) {
        sendToPlayer = toPlayer;
    }

    static void send(ServerPlayer player, int color, float density, int fadeTicks) {
        sendToPlayer.accept(player, new HazeSyncPacket(color, density, fadeTicks));
    }
}
