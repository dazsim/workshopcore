package com.workshopcraft.workshopcore.backdrop;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Server to client: the backdrop id for one dimension, or empty for vanilla. */
public record BackdropSyncPacket(ResourceKey<Level> dimension, Optional<Identifier> id) implements CustomPacketPayload {
    public static final Type<BackdropSyncPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "backdrop_sync"));
    public static final StreamCodec<ByteBuf, BackdropSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.DIMENSION), BackdropSyncPacket::dimension,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), BackdropSyncPacket::id,
            BackdropSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        BackdropClient.acceptAssignment(dimension, id);
    }
}
