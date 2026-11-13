package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket;
import com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket;
import com.workshopcraft.workshopcore.hazard.HazeSyncPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

final class WorkshopCorePackets {
    private WorkshopCorePackets() {
    }

    static void send(ServerPlayer player, CustomPacketPayload payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        if (payload instanceof BackdropSyncPacket packet) {
            packet.write(buf);
        } else if (payload instanceof BackdropDefsPacket packet) {
            packet.write(buf);
        } else if (payload instanceof HazeSyncPacket packet) {
            packet.write(buf);
        } else {
            throw new IllegalArgumentException("Unknown payload " + payload.type().id());
        }
        ServerPlayNetworking.send(player, payload.type().id(), buf);
    }
}
