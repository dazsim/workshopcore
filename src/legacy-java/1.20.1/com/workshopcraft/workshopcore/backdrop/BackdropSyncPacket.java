package com.workshopcraft.workshopcore.backdrop;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Server to client: the backdrop id for one dimension, or empty for vanilla, plus transition ticks. */
public record BackdropSyncPacket(ResourceKey<Level> dimension, Optional<ResourceLocation> id, int transitionTicks) implements CustomPacketPayload {
    public static final Type<BackdropSyncPacket> TYPE = new Type<>(new ResourceLocation(WorkshopCore.MOD_ID, "backdrop_sync"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static BackdropSyncPacket read(FriendlyByteBuf buf) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, buf.readResourceLocation());
        Optional<ResourceLocation> id = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
        return new BackdropSyncPacket(dimension, id, buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension.location());
        buf.writeBoolean(id.isPresent());
        id.ifPresent(buf::writeResourceLocation);
        buf.writeVarInt(transitionTicks);
    }

    public void handle() {
        BackdropClient.acceptAssignment(dimension, id, transitionTicks);
    }
}
