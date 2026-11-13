package com.workshopcraft.workshopcore.hazard;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: the current haze colour, density, and fade length. Density 0 clears. */
public record HazeSyncPacket(int color, float density, int fadeTicks) implements CustomPacketPayload {
    public static final Type<HazeSyncPacket> TYPE = new Type<>(new ResourceLocation(WorkshopCore.MOD_ID, "haze_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static HazeSyncPacket read(FriendlyByteBuf buf) {
        return new HazeSyncPacket(buf.readInt(), buf.readFloat(), buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeFloat(density);
        buf.writeVarInt(fadeTicks);
    }

    public void handle() {
        HazeClient.accept(color, density, fadeTicks);
    }
}
