package com.workshopcraft.workshopcore.backdrop;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Server to client: all loaded backdrop definitions. */
public record BackdropDefsPacket(Map<Identifier, Backdrop> defs) implements CustomPacketPayload {
    public static final Type<BackdropDefsPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "backdrop_defs"));
    public static final StreamCodec<ByteBuf, BackdropDefsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, Backdrop.STREAM_CODEC),
            BackdropDefsPacket::defs,
            BackdropDefsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle() {
        BackdropClient.acceptDefinitions(defs);
    }
}
