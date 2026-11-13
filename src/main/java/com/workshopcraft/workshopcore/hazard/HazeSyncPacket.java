package com.workshopcraft.workshopcore.hazard;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: the current haze colour, density, and fade length. Density 0 clears. */
public record HazeSyncPacket(int color, float density, int fadeTicks) implements CustomPacketPayload {
    public static final Type<HazeSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "haze_sync"));
    public static final StreamCodec<ByteBuf, HazeSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, HazeSyncPacket::color,
            ByteBufCodecs.FLOAT, HazeSyncPacket::density,
            ByteBufCodecs.VAR_INT, HazeSyncPacket::fadeTicks,
            HazeSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        HazeClient.accept(color, density, fadeTicks);
    }
}
